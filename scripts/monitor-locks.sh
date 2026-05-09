#!/bin/bash
set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
INFRA_DIR="$SCRIPT_DIR/../infra"
RESULTS_DIR="$SCRIPT_DIR/../results"

INSTANCE_ID="i-0a0321e1c07df884f"
AWS_REGION="ap-northeast-2"
LOCAL_PORT=3307
INTERVAL=1  # 폴링 간격 (초)

RDS_HOST=$(cd "$INFRA_DIR" && terraform output -raw rds_endpoint)
DB_NAME=$(grep '^db_name' "$INFRA_DIR/terraform.tfvars" | awk -F'"' '{print $2}')
DB_USERNAME=$(grep '^db_username' "$INFRA_DIR/terraform.tfvars" | awk -F'"' '{print $2}')
OUTPUT_FILE="$RESULTS_DIR/lock-$(date +%Y%m%d-%H%M%S).csv"

echo "RDS 엔드포인트: $RDS_HOST"
echo "결과 저장: $OUTPUT_FILE"
echo ""

# SSM 포트 포워딩 시작
echo "SSM 포트 포워딩 시작..."
aws ssm start-session \
  --target "$INSTANCE_ID" \
  --region "$AWS_REGION" \
  --document-name AWS-StartPortForwardingSessionToRemoteHost \
  --parameters "host=$RDS_HOST,portNumber=3306,localPortNumber=$LOCAL_PORT" &
SSM_PID=$!

# 포트 포워딩 준비 대기
for i in {1..15}; do
  if nc -z 127.0.0.1 $LOCAL_PORT 2>/dev/null; then
    echo "포트 포워딩 준비 완료"
    break
  fi
  sleep 1
done

MYSQL_CMD="mysql -h 127.0.0.1 -P $LOCAL_PORT -u $DB_USERNAME -p -N -s 2>/dev/null"

# CSV 헤더 작성
echo "timestamp,active_trx,lock_waits,waiting_sessions,total_processes,max_wait_secs" > "$OUTPUT_FILE"
echo "timestamp,active_trx,lock_waits,waiting_sessions,total_processes,max_wait_secs"
echo "────────────────────────────────────────────────────────────────────"

cleanup() {
  echo ""
  echo "모니터링 종료"
  kill $SSM_PID 2>/dev/null || true
  echo "결과 파일: $OUTPUT_FILE"
}
trap cleanup EXIT

echo "모니터링 시작 (Ctrl+C로 종료)"
echo ""

while true; do
  TS=$(date +%Y-%m-%dT%H:%M:%S)

  ACTIVE_TRX=$(echo "SELECT COUNT(*) FROM information_schema.INNODB_TRX;" \
    | $MYSQL_CMD "$DB_NAME" 2>/dev/null || echo 0)

  LOCK_WAITS=$(echo "SELECT COUNT(*) FROM performance_schema.data_lock_waits;" \
    | $MYSQL_CMD "$DB_NAME" 2>/dev/null || echo 0)

  WAITING_SESSIONS=$(echo "SELECT COUNT(*) FROM information_schema.PROCESSLIST WHERE STATE != '';" \
    | $MYSQL_CMD "$DB_NAME" 2>/dev/null || echo 0)

  TOTAL_PROCESSES=$(echo "SELECT COUNT(*) FROM information_schema.PROCESSLIST;" \
    | $MYSQL_CMD "$DB_NAME" 2>/dev/null || echo 0)

  MAX_WAIT=$(echo "SELECT IFNULL(MAX(REQUESTING_ENGINE_LOCK_ID), 0)
                   FROM performance_schema.data_lock_waits;" \
    | $MYSQL_CMD "$DB_NAME" 2>/dev/null || echo 0)

  echo "$TS,$ACTIVE_TRX,$LOCK_WAITS,$WAITING_SESSIONS,$TOTAL_PROCESSES,$MAX_WAIT" >> "$OUTPUT_FILE"
  printf "%s | trx: %2s | lock_waits: %2s | waiting: %2s | processes: %2s\n" \
    "$TS" "$ACTIVE_TRX" "$LOCK_WAITS" "$WAITING_SESSIONS" "$TOTAL_PROCESSES"

  sleep $INTERVAL
done
