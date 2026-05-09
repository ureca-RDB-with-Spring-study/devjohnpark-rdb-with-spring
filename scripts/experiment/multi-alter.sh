#!/bin/bash
set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
source "$SCRIPT_DIR/../aws/lib/common.sh"

INFRA_DIR="$SCRIPT_DIR/../../infra"
RESULTS_DIR="$SCRIPT_DIR/../../results"
LOADTEST_DIR="$SCRIPT_DIR/../../loadtest"

LOCAL_RDS_PORT=3307
WARMUP_SECS=60
AFTER_SECS=20
K6_SCRIPT="$LOADTEST_DIR/script-experiment.js"

check_aws_credentials

# ── 인프라 정보 수집 ──────────────────────────────────────────────
log_info "인프라 정보 수집..."

APP_INSTANCE_ID=$(get_ec2_instance_id "$APP_NAME")
K6_INSTANCE_ID=$(get_ec2_instance_id "$K6_NAME")

[[ -z "$APP_INSTANCE_ID" || "$APP_INSTANCE_ID" == "None" ]] && { log_error "App EC2 없음 (실행 상태 확인)"; exit 1; }
[[ -z "$K6_INSTANCE_ID"  || "$K6_INSTANCE_ID"  == "None" ]] && { log_error "K6 EC2 없음 (실행 상태 확인)";  exit 1; }

RDS_HOST=$(cd "$INFRA_DIR" && terraform output -raw rds_endpoint)
APP_PRIVATE_IP=$(aws ec2 describe-instances \
  --instance-ids "$APP_INSTANCE_ID" \
  --query "Reservations[0].Instances[0].PrivateIpAddress" \
  --output text --region "$AWS_REGION")

log_info "App EC2 : $APP_INSTANCE_ID ($APP_PRIVATE_IP)"
log_info "K6  EC2 : $K6_INSTANCE_ID"
log_info "RDS     : $RDS_HOST"

# ── DB 접속 정보 ──────────────────────────────────────────────
TFVARS="$INFRA_DIR/terraform.tfvars"
if [[ -f "$TFVARS" ]]; then
  [[ -z "$DB_NAME"     ]] && DB_NAME=$(grep '^db_name' "$TFVARS" | awk -F'"' '{print $2}')
  [[ -z "$DB_USERNAME" ]] && DB_USERNAME=$(grep '^db_username' "$TFVARS" | awk -F'"' '{print $2}')
  [[ -z "$DB_PASSWORD" ]] && DB_PASSWORD=$(grep '^db_password' "$TFVARS" | awk -F'"' '{print $2}')
fi
if [[ -z "$DB_USERNAME" ]]; then
  read -p "RDS DB 유저명: " DB_USERNAME
fi
if [[ -z "$DB_PASSWORD" ]]; then
  read -s -p "RDS DB 비밀번호: " DB_PASSWORD
  echo ""
fi

mkdir -p "$RESULTS_DIR"

# ── SSM 포트포워딩: RDS → localhost:LOCAL_RDS_PORT ──────────────────────────────────────────────
log_info "SSM 포트포워딩 시작 (RDS → localhost:$LOCAL_RDS_PORT)..."
aws ssm start-session \
  --target "$APP_INSTANCE_ID" \
  --region "$AWS_REGION" \
  --document-name AWS-StartPortForwardingSessionToRemoteHost \
  --parameters "host=$RDS_HOST,portNumber=3306,localPortNumber=$LOCAL_RDS_PORT" &
SSM_PID=$!

for i in {1..20}; do
  nc -z 127.0.0.1 $LOCAL_RDS_PORT 2>/dev/null && { log_info "포트포워딩 준비 완료"; break; }
  sleep 1
  [[ $i -eq 20 ]] && { log_error "포트포워딩 타임아웃"; exit 1; }
done

mysql_query() {
  mysql -h 127.0.0.1 -P $LOCAL_RDS_PORT -u "$DB_USERNAME" -p"$DB_PASSWORD" "$DB_NAME" \
    -N -s -e "$1" 2>/dev/null || echo 0
}

# ── MDL 계측 활성화 ──────────────────────────────────────────────
log_info "MDL 계측 활성화..."
mysql_query "UPDATE performance_schema.setup_instruments
  SET ENABLED='YES', TIMED='YES'
  WHERE NAME = 'wait/lock/metadata/sql/mdl';" > /dev/null 2>/dev/null || true

# ── k6 스크립트 전송 + 시작 ──────────────────────────────────────────────
log_info "k6 스크립트 K6 EC2로 전송..."
SCRIPT_B64=$(base64 < "$K6_SCRIPT" | tr -d '\n')

cat > /tmp/k6-start-cmd.json << JSONEOF
{
  "DocumentName": "AWS-RunShellScript",
  "InstanceIds": ["$K6_INSTANCE_ID"],
  "Parameters": {
    "commands": [
      "echo '$SCRIPT_B64' | base64 -d > /tmp/script-experiment.js",
      "nohup k6 run --env APP_URL=http://$APP_PRIVATE_IP /tmp/script-experiment.js > /tmp/k6-experiment.log 2>&1 &",
      "echo \"k6 PID: $!\""
    ]
  }
}
JSONEOF

K6_START_CMD_ID=$(aws ssm send-command \
  --cli-input-json "file:///tmp/k6-start-cmd.json" \
  --region "$AWS_REGION" \
  --query 'Command.CommandId' --output text)

aws ssm wait command-executed \
  --command-id "$K6_START_CMD_ID" \
  --instance-id "$K6_INSTANCE_ID" \
  --region "$AWS_REGION" 2>/dev/null || true

log_info "k6 시작됨 (CommandId: $K6_START_CMD_ID)"

# ── Cleanup ──────────────────────────────────────────────
cleanup() {
  echo ""
  log_info "실험 종료 — 정리 중..."
  [[ -n "$MONITOR_PID" ]] && kill "$MONITOR_PID" 2>/dev/null || true

  # k6 종료
  cat > /tmp/k6-stop-cmd.json << STOPEOF
{
  "DocumentName": "AWS-RunShellScript",
  "InstanceIds": ["$K6_INSTANCE_ID"],
  "Parameters": { "commands": ["pkill k6 2>/dev/null || true"] }
}
STOPEOF
  STOP_CMD_ID=$(aws ssm send-command \
    --cli-input-json "file:///tmp/k6-stop-cmd.json" \
    --region "$AWS_REGION" \
    --query 'Command.CommandId' --output text 2>/dev/null || echo "")

  # k6 로그 수집
  sleep 3
  cat > /tmp/k6-log-cmd.json << LOGEOF
{
  "DocumentName": "AWS-RunShellScript",
  "InstanceIds": ["$K6_INSTANCE_ID"],
  "Parameters": { "commands": ["cat /tmp/k6-experiment.log"] }
}
LOGEOF
  LOG_CMD_ID=$(aws ssm send-command \
    --cli-input-json "file:///tmp/k6-log-cmd.json" \
    --region "$AWS_REGION" \
    --query 'Command.CommandId' --output text 2>/dev/null || echo "")

  if [[ -n "$LOG_CMD_ID" ]]; then
    aws ssm wait command-executed \
      --command-id "$LOG_CMD_ID" \
      --instance-id "$K6_INSTANCE_ID" \
      --region "$AWS_REGION" 2>/dev/null || true

    aws ssm get-command-invocation \
      --command-id "$LOG_CMD_ID" \
      --instance-id "$K6_INSTANCE_ID" \
      --query 'StandardOutputContent' \
      --output text \
      --region "$AWS_REGION" \
      > "$RESULTS_DIR/k6-prod-$(date +%Y%m%d-%H%M%S).log" 2>/dev/null || true

    log_info "k6 로그 저장 완료"
  fi

  kill "$SSM_PID" 2>/dev/null || true
  log_info "결과 위치: $RESULTS_DIR/*-prod-*.csv"
}
trap cleanup EXIT

# ── DB 초기화 ──────────────────────────────────────────────
log_info "DB 상태 초기화..."
ORDER_ID_TYPE=$(mysql_query "SELECT DATA_TYPE FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA='$DB_NAME' AND TABLE_NAME='orders' AND COLUMN_NAME='order_id';")
if [[ "$ORDER_ID_TYPE" == "bigint" ]]; then
  log_info "order_id BIGINT → INT 복원"
  mysql_query "ALTER TABLE orders MODIFY COLUMN order_id INT AUTO_INCREMENT;" > /dev/null
fi
mysql_query "ALTER TABLE orders DROP COLUMN IF EXISTS payment_method;" 2>/dev/null || true
ORDER_COUNT=$(mysql_query "SELECT COUNT(*) FROM orders;")
log_info "현재 orders: ${ORDER_COUNT}건"

# ── 시나리오 실행 함수 ──────────────────────────────────────────────
run_scenario() {
  local scenario_name="$1"
  local alter_sql="$2"
  local output="$RESULTS_DIR/${scenario_name}-prod-$(date +%Y%m%d-%H%M%S).csv"

  echo ""
  echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
  echo " 시나리오: $scenario_name"
  echo " ALTER   : $alter_sql"
  echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"

  echo "timestamp,phase,active_trx,lock_waits,mdl_pending,waiting_sessions,total_processes,max_wait_secs" > "$output"
  printf "%-20s | %-16s | %-9s | %-10s | %-11s | %-8s | %-9s | %s\n" \
    "timestamp" "phase" "active_trx" "lock_waits" "mdl_pending" "waiting" "processes" "max_wait_s"
  echo "─────────────────────────────────────────────────────────────────────────────────────────────────"

  monitor_loop() {
    local phase="$1"
    while true; do
      TS=$(date +%Y-%m-%dT%H:%M:%S)
      ACTIVE_TRX=$(mysql_query "SELECT COUNT(*) FROM information_schema.INNODB_TRX;")
      LOCK_WAITS=$(mysql_query "SELECT COUNT(*) FROM performance_schema.data_lock_waits;")
      MDL_PENDING=$(mysql_query "SELECT COUNT(*) FROM performance_schema.metadata_locks WHERE LOCK_STATUS='PENDING';")
      WAITING=$(mysql_query "SELECT COUNT(*) FROM information_schema.PROCESSLIST WHERE STATE != '';")
      TOTAL=$(mysql_query "SELECT COUNT(*) FROM information_schema.PROCESSLIST;")
      MAX_WAIT=$(mysql_query "SELECT IFNULL(MAX(UNIX_TIMESTAMP(NOW()) - UNIX_TIMESTAMP(trx_wait_started)), 0)
        FROM information_schema.INNODB_TRX WHERE trx_wait_started IS NOT NULL;")

      echo "$TS,$phase,$ACTIVE_TRX,$LOCK_WAITS,$MDL_PENDING,$WAITING,$TOTAL,$MAX_WAIT" >> "$output"
      printf "%-20s | %-16s | %-9s | %-10s | %-11s | %-8s | %-9s | %s\n" \
        "$TS" "$phase" "$ACTIVE_TRX" "$LOCK_WAITS" "$MDL_PENDING" "$WAITING" "$TOTAL" "$MAX_WAIT"
      sleep 1
    done
  }

  monitor_loop "before" &
  MONITOR_PID=$!
  sleep "$WARMUP_SECS"
  kill "$MONITOR_PID" 2>/dev/null; wait "$MONITOR_PID" 2>/dev/null || true

  echo ""
  echo ">>> ALTER 실행: $alter_sql"
  monitor_loop "during" &
  MONITOR_PID=$!
  ALTER_START=$(date +%s)
  mysql_query "$alter_sql" > /dev/null
  ALTER_END=$(date +%s)
  kill "$MONITOR_PID" 2>/dev/null; wait "$MONITOR_PID" 2>/dev/null || true
  echo ">>> ALTER 완료: $((ALTER_END - ALTER_START))초"

  monitor_loop "after" &
  MONITOR_PID=$!
  sleep "$AFTER_SECS"
  kill "$MONITOR_PID" 2>/dev/null; wait "$MONITOR_PID" 2>/dev/null || true

  log_info "결과: $output"
}

# ── 시나리오 1: ADD NOT NULL (COPY) ──────────────────────────────────────────────
run_scenario \
  "01_add_col_not_null_no_default" \
  "ALTER TABLE orders ADD COLUMN payment_method VARCHAR(50) NOT NULL;"
mysql_query "ALTER TABLE orders DROP COLUMN payment_method;" > /dev/null

# ── 시나리오 2: ADD NULL DEFAULT (INSTANT) ──────────────────────────────────────────────
run_scenario \
  "02_add_col_null_default" \
  "ALTER TABLE orders ADD COLUMN payment_method VARCHAR(50) NULL DEFAULT 'UNKNOWN';"
mysql_query "ALTER TABLE orders DROP COLUMN payment_method;" > /dev/null

# ── 시나리오 3: MODIFY INT → BIGINT (COPY) ──────────────────────────────────────────────
run_scenario \
  "03_modify_int_to_bigint" \
  "ALTER TABLE orders MODIFY COLUMN order_id BIGINT AUTO_INCREMENT;"
mysql_query "ALTER TABLE orders MODIFY COLUMN order_id INT AUTO_INCREMENT;" > /dev/null

# ── 시나리오 4: DROP COLUMN (INSTANT) ──────────────────────────────────────────────
mysql_query "ALTER TABLE orders ADD COLUMN payment_method VARCHAR(50) NULL DEFAULT 'UNKNOWN';" > /dev/null
run_scenario \
  "04_drop_column" \
  "ALTER TABLE orders DROP COLUMN payment_method;"

echo ""
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo " 전체 시나리오 완료"
echo " 결과 위치: $RESULTS_DIR/*-prod-*.csv"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
