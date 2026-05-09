#!/bin/bash
set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
YML_FILE="$SCRIPT_DIR/../src/main/resources/application-local.yml"
RESULTS_DIR="$SCRIPT_DIR/../results"
LOADTEST_DIR="$SCRIPT_DIR/../loadtest"

APP_URL="http://localhost:8080"
CUSTOMER_ID=1
PRODUCT_ID=1
TRAFFIC_WARMUP_SECS=120  # k6 300 VU 도달까지 대기 (ramp-up 30s + 90s)
INTERVAL=1

OUTPUT_FILE="$RESULTS_DIR/migration-scenario-$(date +%Y%m%d-%H%M%S).csv"

DB_USER=$(grep 'username:' "$YML_FILE" | awk '{print $2}')
DB_PASSWORD=$(grep 'password:' "$YML_FILE" | awk '{print $2}')

mysql_query() {
  docker exec app-mysql mysql -u"$DB_USER" -p"$DB_PASSWORD" appdb -N -s -e "$1" 2>/dev/null || echo 0
}

cleanup() {
  echo ""
  echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
  echo "시나리오 종료"
  [[ -n "$K6_PID" ]]     && kill "$K6_PID"     2>/dev/null || true
  [[ -n "$MONITOR_PID" ]] && kill "$MONITOR_PID" 2>/dev/null || true
  echo "결과 파일: $OUTPUT_FILE"
}
trap cleanup EXIT

mkdir -p "$RESULTS_DIR"

echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo " 마이그레이션 시나리오 테스트 (로컬)"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo " [1] k6 트래픽 발생 (POST /api/orders)"
echo " [2] ${TRAFFIC_WARMUP_SECS}초 대기 (트래픽 안정화)"
echo " [3] ALTER TABLE orders MODIFY COLUMN order_id BIGINT (COPY) 실행"
echo " [4] 락 모니터링 결과 확인"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo ""

# order_id 타입 확인 — 이미 BIGINT면 INT로 되돌려 재실행 가능하게 함
ORDER_ID_TYPE=$(mysql_query "
  SELECT DATA_TYPE FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA='appdb' AND TABLE_NAME='orders' AND COLUMN_NAME='order_id';")
if [[ "$ORDER_ID_TYPE" == "bigint" ]]; then
  echo "[준비] order_id 이미 BIGINT → INT로 복원 (재실행 정리)..."
  mysql_query "ALTER TABLE orders MODIFY COLUMN order_id INT AUTO_INCREMENT;" > /dev/null
  echo "[준비] 완료"
  echo ""
fi

# 주문 10만건 확인
ORDER_COUNT=$(mysql_query "SELECT COUNT(*) FROM orders;")
echo "[준비] 현재 orders: ${ORDER_COUNT}건"
echo ""

# CSV 헤더
echo "timestamp,phase,active_trx,lock_waits,waiting_sessions,total_processes,max_wait_secs" > "$OUTPUT_FILE"
printf "%-20s | %-12s | %-9s | %-10s | %-8s | %-9s | %s\n" \
  "timestamp" "phase" "active_trx" "lock_waits" "waiting" "processes" "max_wait_s"
echo "──────────────────────────────────────────────────────────────────────────────────"

# 락 모니터링 백그라운드 시작
monitor_loop() {
  local phase="$1"
  while true; do
    TS=$(date +%Y-%m-%dT%H:%M:%S)
    ACTIVE_TRX=$(mysql_query "SELECT COUNT(*) FROM information_schema.INNODB_TRX;")
    LOCK_WAITS=$(mysql_query "SELECT COUNT(*) FROM performance_schema.data_lock_waits;")
    WAITING=$(mysql_query "SELECT COUNT(*) FROM information_schema.PROCESSLIST WHERE STATE != '';")
    TOTAL=$(mysql_query "SELECT COUNT(*) FROM information_schema.PROCESSLIST;")
    MAX_WAIT=$(mysql_query "
      SELECT IFNULL(MAX(UNIX_TIMESTAMP(NOW()) - UNIX_TIMESTAMP(trx_wait_started)), 0)
      FROM information_schema.INNODB_TRX WHERE trx_wait_started IS NOT NULL;")

    echo "$TS,${phase},${ACTIVE_TRX},${LOCK_WAITS},${WAITING},${TOTAL},${MAX_WAIT}" >> "$OUTPUT_FILE"
    printf "%-20s | %-12s | %-9s | %-10s | %-8s | %-9s | %s\n" \
      "$TS" "$phase" "$ACTIVE_TRX" "$LOCK_WAITS" "$WAITING" "$TOTAL" "$MAX_WAIT"
    sleep "$INTERVAL"
  done
}

# [1] k6 시작
echo "[1] k6 트래픽 시작..."
APP_URL="$APP_URL" CUSTOMER_ID="$CUSTOMER_ID" PRODUCT_ID="$PRODUCT_ID" \
  k6 run "$LOADTEST_DIR/script.js" > /tmp/k6-scenario.log 2>&1 &
K6_PID=$!
echo "[1] k6 시작됨 (PID: $K6_PID)"
echo ""

# [2] 트래픽 안정화 대기 + 모니터링
echo "[2] ${TRAFFIC_WARMUP_SECS}초 대기 중 (트래픽 안정화)..."
monitor_loop "before_migration" &
MONITOR_PID=$!

sleep "$TRAFFIC_WARMUP_SECS"

# 모니터링 일시 중지
kill "$MONITOR_PID" 2>/dev/null || true
wait "$MONITOR_PID" 2>/dev/null || true

echo ""
echo "──────────────────────────────────────────────────────────────────────────────────"
echo "[3] ALTER TABLE 실행: orders MODIFY COLUMN order_id BIGINT AUTO_INCREMENT (ALGORITHM=COPY)"
echo "──────────────────────────────────────────────────────────────────────────────────"

# ALTER 실행 + 동시에 모니터링
monitor_loop "during_migration" &
MONITOR_PID=$!

ALTER_START=$(date +%s)
mysql_query "ALTER TABLE orders MODIFY COLUMN order_id BIGINT AUTO_INCREMENT;" > /dev/null
ALTER_END=$(date +%s)
ALTER_DURATION=$((ALTER_END - ALTER_START))

kill "$MONITOR_PID" 2>/dev/null || true
wait "$MONITOR_PID" 2>/dev/null || true

echo ""
echo "──────────────────────────────────────────────────────────────────────────────────"
echo "[3] ALTER TABLE 완료 (소요: ${ALTER_DURATION}초)"
echo "──────────────────────────────────────────────────────────────────────────────────"
echo ""

# [4] ALTER 후 안정화 모니터링
echo "[4] ALTER 후 30초 관찰..."
monitor_loop "after_migration" &
MONITOR_PID=$!
sleep 30
kill "$MONITOR_PID" 2>/dev/null || true
wait "$MONITOR_PID" 2>/dev/null || true

# k6 종료
kill "$K6_PID" 2>/dev/null || true

echo ""
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo " ALTER TABLE 소요시간: ${ALTER_DURATION}초"
echo " 결과 CSV: $OUTPUT_FILE"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
