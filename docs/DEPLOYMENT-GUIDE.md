# CardDemo Transformation - Deployment Guide

**Version**: 1.0
**Date**: 2025-12-30
**Target**: Local Development + Docker Compose

---

## Quick Start (5 minutes)

### Prerequisites
- Docker 24+ & Docker Compose v2
- Git
- 4GB+ RAM available
- Ports available: 3000, 5432, 6379, 8000-8082, 9090, 3001

### Steps

```bash
# 1. Clone repository
cd /home/ubuntu/git/cardTransfo

# 2. Start infrastructure
cd infra/scripts
./start-all.sh

# 3. Wait for all services to be healthy (~2 minutes)
#    Watch progress in terminal

# 4. Access application
# Open browser: http://localhost:3000
# Login: USER0001 / PASSWORD
```

---

## Architecture Overview

```
┌─────────────┐
│   Browser   │ http://localhost:3000
└──────┬──────┘
       │
┌──────▼──────────────────────────────────────────────────┐
│              Kong API Gateway                           │
│              :8000 (proxy) :8001 (admin)                │
└──────┬──────────────┬───────────────────────────────────┘
       │              │
┌──────▼──────┐  ┌───▼────────┐
│ Auth Service│  │Acct Service│
│    :8081    │  │   :8082    │
└──────┬──────┘  └───┬────────┘
       │             │
       └─────┬───────┘
             │
    ┌────────▼─────────┐    ┌──────────┐    ┌──────────┐
    │   PostgreSQL     │    │  Redis   │    │ Keycloak │
    │     :5432        │    │  :6379   │    │  :8080   │
    └──────────────────┘    └──────────┘    └──────────┘
```

---

## Service Details

### 1. PostgreSQL Database

**Purpose**: Replaces VSAM files (USRSEC, ACCTDAT, CUSTDAT)

**Configuration**:
- Host: `localhost` (or `postgresql` from containers)
- Port: `5432`
- Database: `carddemo`
- User: `carddemo`
- Password: `carddemo123`

**Tables**:
- `users` - User authentication (from USRSEC VSAM)
- `customers` - Customer data (from CUSTDAT VSAM)
- `accounts` - Account data (from ACCTDAT VSAM)
- `credit_cards` - Credit card data (from CARDDAT VSAM)
- `transactions` - Transaction history (from TRANDAT VSAM)

**Pre-loaded Data**:
- 7 test users (USER0001-USER0005, ADMIN001-ADMIN002)
- 10 customers
- 12 accounts
- Sample transactions

**Access**:
```bash
# Via docker-compose
docker-compose exec postgresql psql -U carddemo -d carddemo

# Via local psql
psql -h localhost -U carddemo -d carddemo

# Commands
\dt              # List tables
\d+ users        # Describe users table
SELECT * FROM users;
```

---

### 2. Redis Cache

**Purpose**: Session management + data caching

**Configuration**:
- Host: `localhost` (or `redis` from containers)
- Port: `6379`
- Password: `carddemo123`

**Usage**:
- User sessions (JWT tokens)
- Account data cache (TTL: 5 minutes)
- Rate limiting counters

**Access**:
```bash
# Via docker-compose
docker-compose exec redis redis-cli -a carddemo123

# Via local redis-cli
redis-cli -h localhost -p 6379 -a carddemo123

# Commands
KEYS *           # List all keys
GET sessions:*   # View session
TTL sessions:*   # Check TTL
```

---

### 3. Kong API Gateway

**Purpose**: API gateway, rate limiting, JWT validation

**Configuration**:
- Proxy: `http://localhost:8000`
- Admin API: `http://localhost:8001`

**Routes** (to be configured):
```bash
# Add auth-service route
curl -i -X POST http://localhost:8001/services/ \
  --data name=auth-service \
  --data url='http://auth-service:8081'

curl -i -X POST http://localhost:8001/services/auth-service/routes \
  --data 'paths[]=/api/v1/auth' \
  --data name=auth-route

# Add account-service route
curl -i -X POST http://localhost:8001/services/ \
  --data name=account-service \
  --data url='http://account-service:8082'

curl -i -X POST http://localhost:8001/services/account-service/routes \
  --data 'paths[]=/api/v1/accounts' \
  --data name=account-route
```

**Plugins**:
- `rate-limiting`: 100 requests/minute per consumer
- `jwt`: JWT token validation
- `cors`: CORS headers
- `prometheus`: Metrics export

---

### 4. Keycloak

**Purpose**: OAuth2/OIDC authentication server

**Configuration**:
- URL: `http://localhost:8080`
- Admin: `admin / admin123`
- Realm: `carddemo`

**Setup** (to be done manually):
1. Access `http://localhost:8080`
2. Login with admin/admin123
3. Create realm `carddemo`
4. Create client `carddemo-app`
5. Configure JWT settings

---

### 5. Auth Service

**Purpose**: Replaces CICS CC00 transaction (COSGN00C program)

**Configuration**:
- URL: `http://localhost:8081`
- Spring Profile: `docker` (when in container)

**Endpoints**:
- `POST /api/v1/auth/login` - User login
- `POST /api/v1/auth/logout` - User logout
- `GET /api/v1/auth/validate` - Token validation
- `POST /api/v1/auth/refresh` - Refresh token

**API Documentation**:
- Swagger UI: `http://localhost:8081/swagger-ui.html`
- OpenAPI JSON: `http://localhost:8081/api-docs`

**Health Check**:
```bash
curl http://localhost:8081/actuator/health
```

---

### 6. Account Service

**Purpose**: Replaces CICS CACV transaction (COACTVWC program)

**Configuration**:
- URL: `http://localhost:8082`
- Spring Profile: `docker` (when in container)

**Endpoints**:
- `GET /api/v1/accounts/{id}` - Get account details
- `GET /api/v1/accounts/customer/{customerId}` - Get customer accounts
- `GET /api/v1/accounts/search` - Search accounts

**API Documentation**:
- Swagger UI: `http://localhost:8082/swagger-ui.html`

---

### 7. Frontend Application

**Purpose**: Replaces 3270 terminal (BMS mapsets)

**Configuration**:
- URL: `http://localhost:3000`
- Framework: React 18 + TypeScript
- UI Library: Material-UI

**Features**:
- Login page (replaces COSGN00 mapset)
- Dashboard (replaces COMEN01 mapset)
- Account view (replaces COACTVW mapset)

---

### 8. Observability Stack

#### Prometheus
- URL: `http://localhost:9090`
- Scrapes metrics from all services

#### Grafana
- URL: `http://localhost:3001`
- Login: `admin / admin123`
- Pre-configured dashboards for CardDemo

---

## Testing the POC

### 1. Test Login (CC00 equivalent)

```bash
# Via Kong Gateway (production-like)
curl -X POST http://localhost:8000/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "userId": "USER0001",
    "password": "PASSWORD"
  }'

# Direct to service (development)
curl -X POST http://localhost:8081/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "userId": "USER0001",
    "password": "PASSWORD"
  }'

# Expected response:
# {
#   "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
#   "user": {
#     "userId": "USER0001",
#     "firstName": "John",
#     "lastName": "Doe",
#     "userType": "U",
#     "sessionId": "uuid"
#   }
# }
```

### 2. Test Account View (CACV equivalent)

```bash
# Get JWT token from login response first
TOKEN="your-jwt-token-here"

# Via Kong Gateway
curl -X GET http://localhost:8000/api/v1/accounts/1000000001 \
  -H "Authorization: Bearer $TOKEN"

# Direct to service
curl -X GET http://localhost:8082/api/v1/accounts/1000000001 \
  -H "Authorization: Bearer $TOKEN"

# Expected response:
# {
#   "accountId": "1000000001",
#   "status": "ACTIVE",
#   "balances": {
#     "current": 1250.50,
#     "creditLimit": 5000.00,
#     "availableCredit": 3749.50
#   },
#   "customer": {
#     "customerId": 123456789,
#     "fullName": "John Doe"
#   }
# }
```

### 3. Test via Frontend

1. Open `http://localhost:3000`
2. Login with `USER0001` / `PASSWORD`
3. Navigate to dashboard
4. View account details

---

## Troubleshooting

### Services won't start
```bash
# Check Docker
docker ps

# Check logs
docker-compose logs -f auth-service

# Restart specific service
docker-compose restart auth-service
```

### Database connection error
```bash
# Verify PostgreSQL is running
docker-compose ps postgresql

# Check PostgreSQL logs
docker-compose logs postgresql

# Test connection
docker-compose exec postgresql pg_isready -U carddemo
```

### Port already in use
```bash
# Find process using port 8081
lsof -i :8081

# Kill process
kill -9 <PID>

# Or change port in docker-compose.yml
```

---

## Stopping Services

```bash
cd /home/ubuntu/git/cardTransfo/infra/scripts
./stop-all.sh
```

---

## Cleanup (Remove all data)

```bash
cd /home/ubuntu/git/cardTransfo/infra/docker
docker-compose down -v
```

**WARNING**: This will delete all databases and cache data!

---

## Next Steps

1. **Complete TODO items in code**:
   - Implement actual JWT generation in AuthController
   - Add BCrypt password verification
   - Implement Redis session management
   - Add database queries for user authentication

2. **Configure Kong Gateway**:
   - Add service routes
   - Configure JWT plugin
   - Set up rate limiting
   - Enable CORS

3. **Implement Account Service**:
   - Create JPA entities
   - Implement repositories
   - Add business logic
   - Connect to PostgreSQL

4. **Build Frontend**:
   - Create React components
   - Implement login form
   - Add dashboard
   - Integrate with API

5. **Add Tests**:
   - Unit tests
   - Integration tests
   - End-to-end tests

---

**Document Status**: ✅ Ready for Development
**Last Updated**: 2025-12-30
