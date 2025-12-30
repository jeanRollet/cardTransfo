#!/bin/bash
# ============================================================================
# CardDemo Transformation - Start All Services
# ============================================================================

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/../.." && pwd)"
DOCKER_DIR="$PROJECT_ROOT/infra/docker"

echo "============================================"
echo "  CardDemo Transformation - Starting Stack"
echo "============================================"
echo ""

# Colors for output
GREEN='\033[0;32m'
BLUE='\033[0;34m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Check if Docker is running
if ! docker info > /dev/null 2>&1; then
    echo -e "${YELLOW}[ERROR]${NC} Docker is not running. Please start Docker first."
    exit 1
fi

# Check if docker-compose.yml exists
if [ ! -f "$DOCKER_DIR/docker-compose.yml" ]; then
    echo -e "${YELLOW}[ERROR]${NC} docker-compose.yml not found at $DOCKER_DIR"
    exit 1
fi

echo -e "${BLUE}[INFO]${NC} Starting infrastructure services..."
cd "$DOCKER_DIR"

# Start infrastructure first (DB, cache, etc.)
echo -e "${BLUE}[INFO]${NC} Phase 1: Starting database and cache..."
docker-compose up -d postgresql redis kong-database

# Wait for PostgreSQL to be ready
echo -e "${BLUE}[INFO]${NC} Waiting for PostgreSQL to be ready..."
until docker-compose exec -T postgresql pg_isready -U carddemo > /dev/null 2>&1; do
    echo -n "."
    sleep 2
done
echo -e "\n${GREEN}[OK]${NC} PostgreSQL is ready"

# Wait for Redis to be ready
echo -e "${BLUE}[INFO]${NC} Waiting for Redis to be ready..."
until docker-compose exec -T redis redis-cli --raw incr ping > /dev/null 2>&1; do
    echo -n "."
    sleep 2
done
echo -e "\n${GREEN}[OK]${NC} Redis is ready"

# Start Keycloak
echo -e "${BLUE}[INFO]${NC} Phase 2: Starting Keycloak..."
docker-compose up -d keycloak

echo -e "${BLUE}[INFO]${NC} Waiting for Keycloak to be ready (this may take 30-60 seconds)..."
sleep 30

# Start Kong Gateway
echo -e "${BLUE}[INFO]${NC} Phase 3: Starting Kong API Gateway..."
docker-compose up -d kong-migration
sleep 10
docker-compose up -d kong

echo -e "${BLUE}[INFO]${NC} Waiting for Kong to be ready..."
until curl -sf http://localhost:8001 > /dev/null 2>&1; do
    echo -n "."
    sleep 2
done
echo -e "\n${GREEN}[OK]${NC} Kong API Gateway is ready"

# Start microservices
echo -e "${BLUE}[INFO]${NC} Phase 4: Starting microservices..."
docker-compose up -d auth-service account-service

echo -e "${BLUE}[INFO]${NC} Waiting for services to be healthy..."
sleep 20

# Start frontend
echo -e "${BLUE}[INFO]${NC} Phase 5: Starting frontend application..."
docker-compose up -d frontend

# Start observability stack (optional)
echo -e "${BLUE}[INFO]${NC} Phase 6: Starting observability stack..."
docker-compose up -d prometheus grafana

echo ""
echo -e "${GREEN}============================================${NC}"
echo -e "${GREEN}  ✓ All services started successfully!${NC}"
echo -e "${GREEN}============================================${NC}"
echo ""
echo "Service URLs:"
echo "  • Frontend:         http://localhost:3000"
echo "  • Kong Gateway:     http://localhost:8000"
echo "  • Kong Admin:       http://localhost:8001"
echo "  • Auth Service:     http://localhost:8081"
echo "  • Account Service:  http://localhost:8082"
echo "  • Keycloak:         http://localhost:8080"
echo "  • Grafana:          http://localhost:3001 (admin/admin123)"
echo "  • Prometheus:       http://localhost:9090"
echo "  • PostgreSQL:       localhost:5432 (carddemo/carddemo123)"
echo "  • Redis:            localhost:6379"
echo ""
echo "Test credentials:"
echo "  • USER0001 / PASSWORD"
echo "  • ADMIN001 / PASSWORD"
echo ""
echo "Run './stop-all.sh' to stop all services"
echo "Run 'docker-compose logs -f [service]' to view logs"
