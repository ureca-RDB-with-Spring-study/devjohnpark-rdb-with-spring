#!/bin/bash
set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
BULK_SQL="${1:-$SCRIPT_DIR/../sql/bulk-data.sql}"
INFRA_DIR="$SCRIPT_DIR/../infra"

INSTANCE_ID="i-0a0321e1c07df884f"
AWS_REGION="ap-northeast-2"
LOCAL_PORT=3307
DB_NAME=$(grep '^db_name' "$INFRA_DIR/terraform.tfvars" | awk -F'"' '{print $2}')
DB_USERNAME=$(grep '^db_username' "$INFRA_DIR/terraform.tfvars" | awk -F'"' '{print $2}')
DB_PASSWORD=$(grep '^db_password' "$INFRA_DIR/terraform.tfvars" | awk -F'"' '{print $2}')

RDS_HOST=$(cd "$INFRA_DIR" && terraform output -raw rds_endpoint)

echo "RDS 엔드포인트: $RDS_HOST"
echo "SQL 파일: $BULK_SQL"
echo ""
echo "SSM 포트 포워딩 시작..."

aws ssm start-session \
  --target "$INSTANCE_ID" \
  --region "$AWS_REGION" \
  --document-name AWS-StartPortForwardingSessionToRemoteHost \
  --parameters "host=$RDS_HOST,portNumber=3306,localPortNumber=$LOCAL_PORT" &
SSM_PID=$!

for i in {1..15}; do
  if nc -z 127.0.0.1 $LOCAL_PORT 2>/dev/null; then
    echo "포트 포워딩 준비 완료"
    break
  fi
  sleep 1
done

echo "데이터 적재 시작..."
mysql -h 127.0.0.1 -P $LOCAL_PORT -u "$DB_USERNAME" -p"$DB_PASSWORD" "$DB_NAME" < "$BULK_SQL"

echo ""
echo "✓ 대량 데이터 적재 완료"

kill $SSM_PID 2>/dev/null || true
