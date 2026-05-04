#!/bin/bash
set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
source "$SCRIPT_DIR/../lib/common.sh"

check_aws_credentials

log_info "RDS 정지 시도: $RDS_ID"

CURRENT_STATUS=$(get_rds_status)

if [ "$CURRENT_STATUS" = "stopped" ]; then
  log_warn "RDS가 이미 stopped 상태"
  exit 0
fi

if [ "$CURRENT_STATUS" = "stopping" ]; then
  log_warn "RDS가 이미 stopping 중"
  exit 0
fi

if [ "$CURRENT_STATUS" != "available" ]; then
  log_error "RDS가 정지 가능한 상태가 아님 (현재: $CURRENT_STATUS)"
  exit 1
fi

aws rds stop-db-instance \
  --db-instance-identifier "$RDS_ID" \
  --region "$AWS_REGION" \
  --output table 2>&1 | head -10

log_info "✓ 정지 명령 전송. stopped 상태까지 5~10분 소요"
log_warn "  주의: RDS는 7일 후 자동 재시작됨"
