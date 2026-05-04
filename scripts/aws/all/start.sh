#!/bin/bash
set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT_DIR="$SCRIPT_DIR/.."

echo "════════════════════════════════════════"
echo "  smartclearance 인프라 전체 시작"
echo "════════════════════════════════════════"
echo ""

bash "$ROOT_DIR/app/start.sh"
echo ""

bash "$ROOT_DIR/k6/start.sh"
echo ""

bash "$ROOT_DIR/rds/start.sh"
echo ""

echo "════════════════════════════════════════"
echo "✓ 모든 시작 명령 전송 완료"
echo "  EC2: 1~2분, RDS: 5~10분 후 사용 가능"
echo "  상태 확인: ./scripts/all/status.sh"
echo "════════════════════════════════════════"
