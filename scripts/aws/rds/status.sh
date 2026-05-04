#!/bin/bash
set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
source "$SCRIPT_DIR/../lib/common.sh"

check_aws_credentials

aws rds describe-db-instances \
  --db-instance-identifier "$RDS_ID" \
  --query "DBInstances[].[DBInstanceIdentifier,DBInstanceStatus,Engine,EngineVersion,Endpoint.Address]" \
  --output table \
  --region "$AWS_REGION"
