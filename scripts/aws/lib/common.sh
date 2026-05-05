#!/bin/bash
# ─────────────────────────────────────────
# 공용 설정 및 함수
# ─────────────────────────────────────────

# 공통 변수
export AWS_REGION="${AWS_REGION:-ap-northeast-2}"
export PROJECT="smartclearance"

# 인스턴스 식별자
export APP_NAME="${PROJECT}-app"
export K6_NAME="${PROJECT}-k6"
export RDS_ID="${PROJECT}-mysql"

# 색상 (선택, 가독성)
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[0;33m'
NC='\033[0m'

# 로그 함수
log_info() {
  echo -e "${GREEN}[INFO]${NC} $1"
}

log_warn() {
  echo -e "${YELLOW}[WARN]${NC} $1"
}

log_error() {
  echo -e "${RED}[ERROR]${NC} $1"
}

# EC2 인스턴스 ID 동적 조회
get_ec2_instance_id() {
  local name="$1"
  local states="${2:-running,pending,stopping,stopped}"

  aws ec2 describe-instances \
    --filters "Name=tag:Name,Values=${name}" "Name=instance-state-name,Values=${states}" \
    --query "Reservations[0].Instances[0].InstanceId" \
    --output text \
    --region "$AWS_REGION"
}

# EC2 상태 조회
get_ec2_state() {
  local instance_id="$1"

  aws ec2 describe-instances \
    --instance-ids "$instance_id" \
    --query "Reservations[0].Instances[0].State.Name" \
    --output text \
    --region "$AWS_REGION" 2>/dev/null
}

# RDS 상태 조회
get_rds_status() {
  aws rds describe-db-instances \
    --db-instance-identifier "$RDS_ID" \
    --query "DBInstances[0].DBInstanceStatus" \
    --output text \
    --region "$AWS_REGION" 2>/dev/null
}

# 사전 검증 — AWS CLI 자격증명
check_aws_credentials() {
  if ! aws sts get-caller-identity --region "$AWS_REGION" > /dev/null 2>&1; then
    log_error "AWS 자격증명이 설정되지 않았거나 만료됨. 'aws configure' 확인"
    exit 1
  fi
}
