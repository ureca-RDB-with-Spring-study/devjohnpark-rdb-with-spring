#!/bin/bash
set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT_DIR="$SCRIPT_DIR/.."

echo "════════════════════════════════════════"
echo "  smartclearance 인프라 상태"
echo "════════════════════════════════════════"
echo ""
echo "── Spring Boot EC2 ──"
bash "$ROOT_DIR/app/status.sh"
echo ""

echo "── K6 EC2 ──"
bash "$ROOT_DIR/k6/status.sh"
echo ""

echo "── RDS ──"
bash "$ROOT_DIR/rds/status.sh"
echo ""
echo "════════════════════════════════════════"
