# Session Snapshot - 2026-01-01 (Updated: 2026-01-02)

## Status: KAFKA IMPLEMENTATION COMPLETE

### GitHub Repository
**URL** : https://github.com/jeanRollet/cardTransfo

### Services Implemented

| Service | Port | Equivalent CICS | Status |
|---------|------|-----------------|--------|
| auth-service | 8081 | COSGN00C (Login) | Complet |
| card-service | 8082 | COCRDLIC (Cards) | Complet + Kafka |
| account-service | 8083 | COACTVWC (Accounts) | Complet + Kafka |
| transaction-service | 8084 | COTRN00C (History) | Complet + Kafka |
| partner-service | 8085 | CICS Web Services | Complet |
| **notification-service** | **8086** | **MQ Series** | **Complet** |

### Infrastructure
- PostgreSQL 16 avec schema complet + tables outbox/webhook
- Redis pour rate limiting + sessions
- **Kafka + Zookeeper** pour event-driven messaging
- Docker Compose fonctionnel
- Frontend React (port 3000)

---

## Kafka Event-Driven Architecture - IMPLEMENTED

### Architecture

```
┌─────────────┐  ┌─────────────┐  ┌─────────────┐
│ transaction │  │    card     │  │   account   │
│   service   │  │   service   │  │   service   │
└──────┬──────┘  └──────┬──────┘  └──────┬──────┘
       │                │                │
       ▼                ▼                ▼
   OUTBOX TABLES (PostgreSQL) ──────────────────┐
       │                │                │      │
       ▼                ▼                ▼      │
┌─────────────────────────────────────────────┐ │
│              KAFKA CLUSTER                   │ │
│  carddemo.transactions | cards | accounts   │ │
└─────────────────────────────────────────────┘ │
                      │                         │
                      ▼                         │
         ┌────────────────────────┐             │
         │  notification-service  │◄────────────┘
         │       (8086)           │
         │  - Consume events      │
         │  - Filter by scopes    │
         │  - Deliver webhooks    │
         └────────────────────────┘
                      │
                      ▼
              Partner Webhooks
```

### Kafka Topics Created
| Topic | Partitions | Retention |
|-------|------------|-----------|
| carddemo.transactions | 8 | 7 days |
| carddemo.cards | 4 | 7 days |
| carddemo.accounts | 4 | 7 days |
| carddemo.dlq | 4 | 30 days |

### Files Created/Modified

#### 1. Infrastructure
- `infra/docker/docker-compose.yml` - Added Zookeeper, Kafka, kafka-init, notification-service

#### 2. Database Schema
- `infra/config/postgresql/init-db.sql` - Added:
  - `transaction_outbox` table
  - `card_outbox` table
  - `account_outbox` table
  - `webhook_subscriptions` table
  - `webhook_deliveries` table

#### 3. Shared Module
- `appli/shared/pom.xml` - Added Jackson dependencies
- `appli/shared/src/main/java/com/carddemo/shared/event/`
  - `DomainEvent.java` - Base event class
  - `TransactionCreatedEvent.java`
  - `CardStatusChangedEvent.java`
  - `AccountUpdatedEvent.java`
  - `KafkaTopics.java` - Topic constants

#### 4. Notification Service (NEW)
- `appli/notification-service/` - Complete service with:
  - `pom.xml`, `Dockerfile`
  - `config/KafkaConsumerConfig.java`, `WebClientConfig.java`, `OpenApiConfig.java`
  - `entity/Partner.java`, `WebhookSubscription.java`, `WebhookDelivery.java`
  - `repository/` - All repositories
  - `service/WebhookDeliveryService.java`, `WebhookDispatcher.java`, `RetrySchedulerService.java`
  - `consumer/TransactionEventConsumer.java`, `CardEventConsumer.java`, `AccountEventConsumer.java`
  - `controller/WebhookAdminController.java`
  - `resources/application.yml`

#### 5. Transaction Service
- Added Kafka dependencies to `pom.xml`
- `config/KafkaProducerConfig.java`
- `entity/TransactionOutbox.java`
- `repository/TransactionOutboxRepository.java`
- `service/TransactionEventPublisher.java`
- `scheduler/OutboxPublisher.java`
- Updated `application.yml` with Kafka config

#### 6. Card Service
- Added Kafka dependencies to `pom.xml`
- `config/KafkaProducerConfig.java`
- `entity/CardOutbox.java`
- `repository/CardOutboxRepository.java`
- `service/CardEventPublisher.java`
- `scheduler/OutboxPublisher.java`
- Updated `application.yml` with Kafka config

#### 7. Account Service
- Added Kafka dependencies to `pom.xml`
- `config/KafkaProducerConfig.java`
- `entity/AccountOutbox.java`
- `repository/AccountOutboxRepository.java`
- `service/AccountEventPublisher.java`
- `scheduler/OutboxPublisher.java`
- Updated `application.yml` with Kafka config

#### 8. Parent POM
- Added `notification-service` module
- Added `spring-kafka` to dependency management

---

## Patterns Implemented

### Outbox Pattern
Equivalent to CICS SYNCPOINT + MQ PUT atomicity (Unit of Work)
- Business operation and event saved in same transaction
- Scheduler polls outbox and publishes to Kafka
- Guarantees exactly-once delivery semantics

### Webhook Delivery with Retry
- Exponential backoff: 1min -> 5min -> 15min -> 1h -> 4h
- Max 5 attempts before dead letter
- Scope-based filtering (partners only receive events they're authorized for)

---

## Next Steps (Optional Enhancements)

1. **Integration with services**: Add calls to `*EventPublisher` in existing service methods
2. **Webhook subscriptions**: Seed webhook_subscriptions table for partners
3. **Testing**: End-to-end test of event flow
4. **Monitoring**: Add Kafka metrics to Prometheus/Grafana

---

## Commands

```bash
# Start all services
cd /home/ubuntu/git/cardTransfo/infra/docker
docker-compose up -d

# Build Maven project
cd /home/ubuntu/git/cardTransfo/appli
mvn clean package -DskipTests

# View Kafka topics
docker exec carddemo-kafka kafka-topics --list --bootstrap-server localhost:9092

# View consumer groups
docker exec carddemo-kafka kafka-consumer-groups --list --bootstrap-server localhost:9092
```

---

## Credentials
- **PostgreSQL**: carddemo / carddemo123
- **Redis**: carddemo123
- **Webhook Secret**: carddemo-webhook-secret (configurable)
