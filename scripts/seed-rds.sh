#!/bin/bash
set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
SEED_SQL="$SCRIPT_DIR/../sql/seed-data.sql"
INFRA_DIR="$SCRIPT_DIR/../infra"

LOCAL_PORT=3307
INSTANCE_ID="i-0a0321e1c07df884f"
AWS_REGION="ap-northeast-2"

RDS_HOST=$(cd "$INFRA_DIR" && terraform output -raw rds_endpoint)
DB_NAME="appdb"
DB_USERNAME="app"

echo "RDS 엔드포인트: $RDS_HOST"
echo "SSM 포트 포워딩 시작 (localhost:$LOCAL_PORT → $RDS_HOST:3306)"

aws ssm start-session \
  --target "$INSTANCE_ID" \
  --region "$AWS_REGION" \
  --document-name AWS-StartPortForwardingSessionToRemoteHost \
  --parameters "host=$RDS_HOST,portNumber=3306,localPortNumber=$LOCAL_PORT" &

SSM_PID=$!

echo "포트 포워딩 대기 중..."
for i in {1..15}; do
  if nc -z 127.0.0.1 $LOCAL_PORT 2>/dev/null; then
    echo "포트 포워딩 준비 완료"
    break
  fi
  sleep 1
done

echo "시드 데이터 업로드 중..."
mysql -h 127.0.0.1 -P $LOCAL_PORT -u "$DB_USERNAME" -p "$DB_NAME" < "$SEED_SQL"

echo "✓ 시드 데이터 업로드 완료"

kill $SSM_PID 2>/dev/null || true
