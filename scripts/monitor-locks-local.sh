#!/bin/bash
set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
YML_FILE="$SCRIPT_DIR/../src/main/resources/application-local.yml"
RESULTS_DIR="$SCRIPT_DIR/../results"

INTERVAL=1
OUTPUT_FILE="$RESULTS_DIR/lock-local-$(date +%Y%m%d-%H%M%S).csv"

# .env에서 비밀번호 추출 (특수문자 보존)
DB_USER=$(grep 'username:' "$YML_FILE" | awk '{print $2}')
DB_PASSWORD=$(grep 'password:' "$YML_FILE" | awk '{print $2}')

if ! docker ps --format '{{.Names}}' | grep -q '^app-mysql$'; then
  echo "app-mysql 컨테이너가 실행 중이지 않습니다."
  exit 1
fi

mkdir -p "$RESULTS_DIR"

mysql_query() {
  docker exec app-mysql mysql -u"$DB_USER" -p"$DB_PASSWORD" appdb -N -s -e "$1" 2>/dev/null || echo 0
}

echo "결과 저장: $OUTPUT_FILE"
echo ""

echo "timestamp,active_trx,lock_waits,waiting_sessions,total_processes,max_wait_secs" > "$OUTPUT_FILE"
printf "%-20s | %-9s | %-10s | %-8s | %-9s | %s\n" \
  "timestamp" "active_trx" "lock_waits" "waiting" "processes" "max_wait_s"
echo "────────────────────────────────────────────────────────────────────"

cleanup() {
  echo ""
  echo "모니터링 종료. 결과 파일: $OUTPUT_FILE"
}
trap cleanup EXIT

echo "모니터링 시작 (Ctrl+C로 종료)"
echo ""

while true; do
  TS=$(date +%Y-%m-%dT%H:%M:%S)

  ACTIVE_TRX=$(mysql_query "SELECT COUNT(*) FROM information_schema.INNODB_TRX;")

  LOCK_WAITS=$(mysql_query "SELECT COUNT(*) FROM performance_schema.data_lock_waits;")

  WAITING_SESSIONS=$(mysql_query "SELECT COUNT(*) FROM information_schema.PROCESSLIST WHERE STATE != '';")

  TOTAL_PROCESSES=$(mysql_query "SELECT COUNT(*) FROM information_schema.PROCESSLIST;")

  MAX_WAIT=$(mysql_query "
    SELECT IFNULL(MAX(UNIX_TIMESTAMP(NOW()) - UNIX_TIMESTAMP(trx_wait_started)), 0)
    FROM information_schema.INNODB_TRX
    WHERE trx_wait_started IS NOT NULL;")

  echo "$TS,$ACTIVE_TRX,$LOCK_WAITS,$WAITING_SESSIONS,$TOTAL_PROCESSES,$MAX_WAIT" >> "$OUTPUT_FILE"
  printf "%-20s | %-9s | %-10s | %-8s | %-9s | %s\n" \
    "$TS" "$ACTIVE_TRX" "$LOCK_WAITS" "$WAITING_SESSIONS" "$TOTAL_PROCESSES" "$MAX_WAIT"

  sleep $INTERVAL
done
