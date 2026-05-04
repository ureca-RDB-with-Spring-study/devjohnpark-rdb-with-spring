#!/bin/bash
set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
source "$SCRIPT_DIR/../lib/common.sh"

check_aws_credentials

log_info "RDS 시작 시도: $RDS_ID"

CURRENT_STATUS=$(get_rds_status)

if [ "$CURRENT_STATUS" = "available" ]; then
  log_warn "RDS가 이미 available 상태"
  exit 0
fi

if [ "$CURRENT_STATUS" = "starting" ]; then
  log_warn "RDS가 이미 starting 중"
  exit 0
fi

aws rds start-db-instance \
  --db-instance-identifier "$RDS_ID" \
  --region "$AWS_REGION" \
  --output table 2>&1 | head -10

log_info "✓ 시작 명령 전송. available 상태까지 5~10분 소요"
log_info "  상태 확인: ./scripts/rds/status.sh"
