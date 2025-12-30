#!/bin/bash
# ============================================================================
# CardDemo Transformation - Stop All Services
# ============================================================================

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/../.." && pwd)"
DOCKER_DIR="$PROJECT_ROOT/infra/docker"

echo "============================================"
echo "  CardDemo Transformation - Stopping Stack"
echo "============================================"
echo ""

cd "$DOCKER_DIR"

echo "[INFO] Stopping all services..."
docker-compose down

echo ""
echo "✓ All services stopped"
echo ""
echo "To remove volumes (WARNING: deletes all data):"
echo "  docker-compose down -v"
