#!/bin/bash
set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT_DIR="$SCRIPT_DIR/.."

echo "════════════════════════════════════════"
echo "  smartclearance 인프라 전체 정지"
echo "════════════════════════════════════════"
echo ""

bash "$ROOT_DIR/app/stop.sh"
echo ""

bash "$ROOT_DIR/k6/stop.sh"
echo ""

bash "$ROOT_DIR/rds/stop.sh"
echo ""

echo "════════════════════════════════════════"
echo "✓ 모든 정지 명령 전송 완료"
echo "════════════════════════════════════════"
