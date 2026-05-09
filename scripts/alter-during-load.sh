#!/bin/bash
set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ENV_FILE="$SCRIPT_DIR/../.env"

DB_USER=$(grep '^MYSQL_USER=' "$ENV_FILE" | cut -d'=' -f2-)
DB_PASSWORD=$(grep '^MYSQL_PASSWORD=' "$ENV_FILE" | cut -d'=' -f2-)

WAIT_SECS=${1:-40}  # k6 ramp-up(30s) + 여유 10s

echo "=== ALTER-DURING-LOAD 테스트 ==="
echo ""
echo "[1] 터미널 1에서 모니터 실행 중인지 확인하세요."
echo "    ./scripts/monitor-locks-local.sh"
echo ""
echo "[2] 터미널 2에서 k6 실행 중인지 확인하세요."
echo "    k6 run --env APP_URL=http://localhost:8080 loadtest/script.js"
echo ""
read -p "두 터미널 모두 실행 중이면 Enter를 누르세요..."

echo ""
echo "k6 ramp-up 대기 중... (${WAIT_SECS}초 후 ALTER TABLE 실행)"

for i in $(seq "$WAIT_SECS" -1 1); do
  printf "\r  ALTER TABLE까지 %2d초..." "$i"
  sleep 1
done

echo ""
echo ""
echo ">>> ALTER TABLE orders ADD COLUMN payment_method 실행"

docker exec app-mysql mysql -u"$DB_USER" -p"$DB_PASSWORD" appdb \
  -e "ALTER TABLE orders ADD COLUMN payment_method VARCHAR(50) NULL DEFAULT 'UNKNOWN';" 2>/dev/null

echo ">>> 완료. 모니터 CSV에서 변화를 확인하세요."
