#!/bin/bash
set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
YML_FILE="$SCRIPT_DIR/../src/main/resources/application-local.yml"
RESULTS_DIR="$SCRIPT_DIR/../results"
LOADTEST_DIR="$SCRIPT_DIR/../loadtest"

APP_URL="http://localhost:8080"
CUSTOMER_ID=1
PRODUCT_ID=1
WARMUP_SECS=60    # k6 안정화 대기
AFTER_SECS=20     # ALTER 완료 후 관찰
K6_PID=""

DB_USER=$(grep 'username:' "$YML_FILE" | awk '{print $2}')
DB_PASSWORD=$(grep 'password:' "$YML_FILE" | awk '{print $2}')

mysql_query() {
  docker exec app-mysql mysql -u"$DB_USER" -p"$DB_PASSWORD" appdb -N -s -e "$1" 2>/dev/null || echo 0
}

cleanup() {
  echo ""
  [[ -n "$K6_PID" ]] && kill "$K6_PID" 2>/dev/null || true
  [[ -n "$MONITOR_PID" ]] && kill "$MONITOR_PID" 2>/dev/null || true
}
trap cleanup EXIT

mkdir -p "$RESULTS_DIR"

# ────────────────────────────────────────────────
run_scenario() {
  local scenario_name="$1"
  local alter_sql="$2"
  local output="$RESULTS_DIR/${scenario_name}-$(date +%Y%m%d-%H%M%S).csv"

  echo ""
  echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
  echo " 시나리오: $scenario_name"
  echo " ALTER   : $alter_sql"
  echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"

  echo "timestamp,phase,active_trx,lock_waits,waiting_sessions,total_processes,max_wait_secs" > "$output"
  printf "%-20s | %-16s | %-9s | %-10s | %-8s | %-9s | %s\n" \
    "timestamp" "phase" "active_trx" "lock_waits" "waiting" "processes" "max_wait_s"
  echo "─────────────────────────────────────────────────────────────────────────────────────"

  monitor_loop() {
    local phase="$1"
    while true; do
      TS=$(date +%Y-%m-%dT%H:%M:%S)
      ACTIVE_TRX=$(mysql_query "SELECT COUNT(*) FROM information_schema.INNODB_TRX;")
      LOCK_WAITS=$(mysql_query "SELECT COUNT(*) FROM performance_schema.data_lock_waits;")
      WAITING=$(mysql_query "SELECT COUNT(*) FROM information_schema.PROCESSLIST WHERE STATE != '';")
      TOTAL=$(mysql_query "SELECT COUNT(*) FROM information_schema.PROCESSLIST;")
      MAX_WAIT=$(mysql_query "SELECT IFNULL(MAX(UNIX_TIMESTAMP(NOW()) - UNIX_TIMESTAMP(trx_wait_started)), 0)
        FROM information_schema.INNODB_TRX WHERE trx_wait_started IS NOT NULL;")
      echo "$TS,$phase,$ACTIVE_TRX,$LOCK_WAITS,$WAITING,$TOTAL,$MAX_WAIT" >> "$output"
      printf "%-20s | %-16s | %-9s | %-10s | %-8s | %-9s | %s\n" \
        "$TS" "$phase" "$ACTIVE_TRX" "$LOCK_WAITS" "$WAITING" "$TOTAL" "$MAX_WAIT"
      sleep 1
    done
  }

  # before
  monitor_loop "before" &
  MONITOR_PID=$!
  sleep "$WARMUP_SECS"
  kill "$MONITOR_PID" 2>/dev/null; wait "$MONITOR_PID" 2>/dev/null || true

  # during
  echo ""
  echo ">>> ALTER 실행"
  monitor_loop "during" &
  MONITOR_PID=$!
  ALTER_START=$(date +%s)
  mysql_query "$alter_sql" > /dev/null
  ALTER_END=$(date +%s)
  kill "$MONITOR_PID" 2>/dev/null; wait "$MONITOR_PID" 2>/dev/null || true

  echo ">>> ALTER 완료: $((ALTER_END - ALTER_START))초"

  # after
  monitor_loop "after" &
  MONITOR_PID=$!
  sleep "$AFTER_SECS"
  kill "$MONITOR_PID" 2>/dev/null; wait "$MONITOR_PID" 2>/dev/null || true

  echo " 결과: $output"
}
# ────────────────────────────────────────────────

# DB 상태 초기화
echo "[초기화] order_id 타입 및 컬럼 상태 확인..."
ORDER_ID_TYPE=$(mysql_query "SELECT DATA_TYPE FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA='appdb' AND TABLE_NAME='orders' AND COLUMN_NAME='order_id';")
if [[ "$ORDER_ID_TYPE" == "bigint" ]]; then
  echo "  order_id BIGINT → INT 복원"
  mysql_query "ALTER TABLE orders MODIFY COLUMN order_id INT AUTO_INCREMENT;" > /dev/null
fi
mysql_query "ALTER TABLE orders DROP COLUMN IF EXISTS payment_method;" > /dev/null 2>&1 || true
echo "[초기화] 완료"
echo ""

# k6 시작 (전체 시나리오 동안 유지)
echo "[k6] 트래픽 시작..."
APP_URL="$APP_URL" CUSTOMER_ID="$CUSTOMER_ID" PRODUCT_ID="$PRODUCT_ID" \
  k6 run "$LOADTEST_DIR/script.js" > /tmp/k6-multi.log 2>&1 &
K6_PID=$!
echo "[k6] PID: $K6_PID"
echo ""

# ── 시나리오 1: ADD COLUMN NOT NULL (DEFAULT 없음) → COPY ──
run_scenario \
  "01_add_col_not_null_no_default" \
  "ALTER TABLE orders ADD COLUMN payment_method VARCHAR(50) NOT NULL;"

mysql_query "ALTER TABLE orders DROP COLUMN payment_method, ALGORITHM=INSTANT;" > /dev/null

# ── 시나리오 2: ADD COLUMN NULL DEFAULT → INSTANT ──
run_scenario \
  "02_add_col_null_default" \
  "ALTER TABLE orders ADD COLUMN payment_method VARCHAR(50) NULL DEFAULT 'UNKNOWN';"

mysql_query "ALTER TABLE orders DROP COLUMN payment_method, ALGORITHM=INSTANT;" > /dev/null

# ── 시나리오 3: MODIFY order_id INT → BIGINT → COPY ──
run_scenario \
  "03_modify_int_to_bigint" \
  "ALTER TABLE orders MODIFY COLUMN order_id BIGINT AUTO_INCREMENT;"

mysql_query "ALTER TABLE orders MODIFY COLUMN order_id INT AUTO_INCREMENT;" > /dev/null

# ── 시나리오 4: DROP COLUMN → INSTANT ──
mysql_query "ALTER TABLE orders ADD COLUMN payment_method VARCHAR(50) NULL DEFAULT 'UNKNOWN', ALGORITHM=INSTANT;" > /dev/null
run_scenario \
  "04_drop_column" \
  "ALTER TABLE orders DROP COLUMN payment_method;"

# k6 종료
kill "$K6_PID" 2>/dev/null || true

echo ""
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo " 전체 시나리오 완료"
echo " 결과 위치: $RESULTS_DIR"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
