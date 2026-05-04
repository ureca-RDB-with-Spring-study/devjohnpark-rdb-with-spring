#!/bin/bash
set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
source "$SCRIPT_DIR/../lib/common.sh"

check_aws_credentials

aws ec2 describe-instances \
  --filters "Name=tag:Name,Values=${K6_NAME}" \
  --query "Reservations[].Instances[].[Tags[?Key==\`Name\`]|[0].Value,InstanceId,State.Name,PublicIpAddress]" \
  --output table \
  --region "$AWS_REGION"
