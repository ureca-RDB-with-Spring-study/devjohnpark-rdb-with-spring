#!/bin/bash
set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
source "$SCRIPT_DIR/../lib/common.sh"

check_aws_credentials

log_info "K6 EC2 시작 시도..."

INSTANCE_ID=$(get_ec2_instance_id "$K6_NAME" "stopped,stopping")

if [ -z "$INSTANCE_ID" ] || [ "$INSTANCE_ID" = "None" ]; then
  log_warn "K6 EC2가 이미 실행 중이거나 찾을 수 없음"
  exit 0
fi

log_info "인스턴스 ID: $INSTANCE_ID"

aws ec2 start-instances \
  --instance-ids "$INSTANCE_ID" \
  --region "$AWS_REGION" \
  --output table

log_info "✓ 시작 명령 전송. running 상태까지 1~2분 소요"
