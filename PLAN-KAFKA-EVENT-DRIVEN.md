# Plan: Architecture Event-Driven Kafka pour CardDemo

## Objectif
Implémenter une architecture événementielle avec Kafka pour remplacer les patterns CICS/MQ Series du mainframe, incluant les notifications webhook aux partenaires.

## Architecture Cible

```
┌─────────────────┐  ┌─────────────────┐  ┌─────────────────┐
│  transaction    │  │     card        │  │    account      │
│    service      │  │    service      │  │    service      │
└────────┬────────┘  └────────┬────────┘  └────────┬────────┘
         │                    │                    │
         │ TransactionCreated │ CardStatusChanged  │ AccountUpdated
         │                    │                    │
         ▼                    ▼                    ▼
    ┌─────────────────────────────────────────────────────┐
    │               OUTBOX TABLES (PostgreSQL)            │
    │  transaction_outbox | card_outbox | account_outbox  │
    └─────────────────────────────────────────────────────┘
         │                    │                    │
         ▼                    ▼                    ▼
    ┌─────────────────────────────────────────────────────┐
    │                    KAFKA CLUSTER                     │
    │  carddemo.transactions | carddemo.cards | carddemo.accounts │
    └─────────────────────────────────────────────────────┘
                              │
                              ▼
                 ┌────────────────────────┐
                 │  notification-service  │
                 │       (8086)           │
                 │  - Consume events      │
                 │  - Filter by scopes    │
                 │  - Deliver webhooks    │
                 │  - Retry + backoff     │
                 └────────────────────────┘
                              │
                              ▼
                    Partner Webhooks
```

## Équivalence Mainframe → Cloud Native

| CICS/MQ Series | Kafka |
|----------------|-------|
| MQ Queue | Topic Kafka |
| Queue Manager | Broker Kafka |
| MQ PUT | KafkaTemplate.send() |
| MQ GET | @KafkaListener |
| Dead Letter Queue | carddemo.dlq |
| Transaction Scope | Outbox Pattern |

---

## Fichiers à Créer/Modifier

### 1. Infrastructure Docker

**Modifier:** `infra/docker/docker-compose.yml`
- Ajouter Zookeeper (confluentinc/cp-zookeeper:7.5.0)
- Ajouter Kafka (confluentinc/cp-kafka:7.5.0)
- Ajouter kafka-init pour créer les topics
- Ajouter notification-service
- Mettre à jour les dépendances des services existants

### 2. Schéma Base de Données

**Modifier:** `infra/config/postgresql/init-db.sql`

```sql
-- Tables Outbox (par service)
CREATE TABLE transaction_outbox (
    id BIGSERIAL PRIMARY KEY,
    event_id UUID NOT NULL UNIQUE,
    event_type VARCHAR(50) NOT NULL,
    aggregate_id VARCHAR(50) NOT NULL,
    payload JSONB NOT NULL,
    status VARCHAR(20) DEFAULT 'PENDING',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    published_at TIMESTAMP
);

CREATE TABLE card_outbox (...);
CREATE TABLE account_outbox (...);

-- Tables Webhook
CREATE TABLE webhook_deliveries (
    delivery_id BIGSERIAL PRIMARY KEY,
    event_id UUID NOT NULL,
    partner_id INTEGER REFERENCES partners(partner_id),
    webhook_url VARCHAR(255) NOT NULL,
    payload JSONB NOT NULL,
    status VARCHAR(20) DEFAULT 'PENDING',
    attempt_count INTEGER DEFAULT 0,
    next_attempt_at TIMESTAMP,
    last_error VARCHAR(500)
);

CREATE TABLE webhook_subscriptions (
    subscription_id SERIAL PRIMARY KEY,
    partner_id INTEGER REFERENCES partners(partner_id),
    event_type VARCHAR(50) NOT NULL,
    scope_required VARCHAR(50) NOT NULL,
    UNIQUE(partner_id, event_type)
);
```

### 3. Module Shared - Event DTOs

**Créer:** `appli/shared/src/main/java/com/carddemo/shared/event/`

- `DomainEvent.java` - Classe de base avec eventId, eventType, schemaVersion, occurredAt
- `TransactionCreatedEvent.java` - Événement transaction créée
- `CardStatusChangedEvent.java` - Événement changement statut carte
- `AccountUpdatedEvent.java` - Événement mise à jour compte

### 4. Transaction Service - Producteur

**Modifier:** `appli/transaction-service/pom.xml`
- Ajouter spring-kafka dependency

**Créer:**
- `entity/TransactionOutbox.java`
- `repository/TransactionOutboxRepository.java`
- `service/TransactionEventPublisher.java`
- `scheduler/OutboxPublisher.java`
- `config/KafkaProducerConfig.java`

**Modifier:** `resources/application.yml`
- Ajouter configuration Kafka

### 5. Card Service - Producteur

**Modifier:** `appli/card-service/pom.xml`

**Créer:**
- `entity/CardOutbox.java`
- `repository/CardOutboxRepository.java`
- `service/CardEventPublisher.java`
- `scheduler/OutboxPublisher.java`

**Modifier:** `service/CardService.java`
- Appeler `cardEventPublisher.publishCardStatusChanged()` dans `updateCardStatus()`

### 6. Account Service - Producteur

**Modifier:** `appli/account-service/pom.xml`

**Créer:**
- `entity/AccountOutbox.java`
- `repository/AccountOutboxRepository.java`
- `service/AccountEventPublisher.java`
- `scheduler/OutboxPublisher.java`

### 7. Notification Service (NOUVEAU - Port 8086)

**Créer:** `appli/notification-service/`

```
notification-service/
├── pom.xml
├── Dockerfile
└── src/main/
    ├── java/com/carddemo/notification/
    │   ├── NotificationServiceApplication.java
    │   ├── config/
    │   │   ├── KafkaConsumerConfig.java
    │   │   └── WebClientConfig.java
    │   ├── consumer/
    │   │   ├── TransactionEventConsumer.java
    │   │   ├── CardEventConsumer.java
    │   │   └── AccountEventConsumer.java
    │   ├── entity/
    │   │   ├── WebhookDelivery.java
    │   │   └── WebhookSubscription.java
    │   ├── repository/
    │   │   ├── WebhookDeliveryRepository.java
    │   │   └── WebhookSubscriptionRepository.java
    │   ├── service/
    │   │   ├── WebhookDeliveryService.java
    │   │   ├── WebhookDispatcher.java
    │   │   └── RetrySchedulerService.java
    │   └── controller/
    │       └── WebhookAdminController.java
    └── resources/
        └── application.yml
```

### 8. Parent POM

**Modifier:** `appli/pom.xml`
- Ajouter module notification-service
- Ajouter spring-kafka dans dependencyManagement

---

## Topics Kafka

| Topic | Partitions | Rétention | Clé |
|-------|------------|-----------|-----|
| carddemo.transactions | 8 | 7 jours | accountId |
| carddemo.cards | 4 | 7 jours | cardNumber |
| carddemo.accounts | 4 | 7 jours | accountId |
| carddemo.dlq | 4 | 30 jours | eventId |

---

## Mécanisme de Retry Webhook

Backoff exponentiel: **1min → 5min → 15min → 1h → 4h**

```java
// WebhookDispatcher.java
private static final int[] BACKOFF_MINUTES = {1, 5, 15, 60, 240};

if (attemptCount >= maxAttempts) {
    status = "DEAD_LETTER";
} else {
    nextAttemptAt = now().plusMinutes(BACKOFF_MINUTES[attemptCount - 1]);
}
```

---

## Filtrage par Scopes

Les partenaires reçoivent uniquement les événements correspondant à leurs scopes:

| Événement | Scope Requis |
|-----------|--------------|
| TransactionCreated | transactions:read |
| CardStatusChanged | cards:read |
| AccountUpdated | accounts:read |

Exemple: Fintech ABC (scopes: accounts:read, transactions:read)
- ✅ Reçoit TransactionCreated
- ❌ Ne reçoit PAS CardStatusChanged
- ✅ Reçoit AccountUpdated

---

## Décisions Utilisateur

- **Transactions**: Endpoint de simulation uniquement (pas de POST réel)
- **Sécurité Webhook**: Header simple `X-Webhook-Secret` (pas HMAC)
- **Pattern Outbox = UOW CICS**: L'écriture dans outbox fait partie de la même transaction DB que l'opération métier (équivalent SYNCPOINT)

## Équivalence UOW CICS → Outbox Pattern

```
CICS UOW (Unit of Work):            Spring @Transactional + Outbox:
┌─────────────────────────┐         ┌─────────────────────────┐
│ EXEC CICS START         │         │ @Transactional          │
│   UPDATE DB2 TABLE      │   ═══>  │   repository.save()     │
│   MQ PUT MESSAGE        │         │   outbox.save(event)    │
│ EXEC CICS SYNCPOINT     │         │ } // commit atomique    │
└─────────────────────────┘         └─────────────────────────┘
        │                                    │
        │ ABEND = ROLLBACK ALL               │ Exception = ROLLBACK ALL
        ▼                                    ▼
   Message jamais envoyé              Event jamais publié
```

---

## Ordre d'Implémentation

1. **Infrastructure Kafka** - docker-compose.yml, topics
2. **Schéma DB** - Tables outbox et webhook
3. **Shared module** - Event DTOs
4. **notification-service** - Nouveau service complet
5. **transaction-service** - Outbox + publisher + endpoint simulation
6. **card-service** - Outbox + publisher
7. **account-service** - Outbox + publisher
8. **Tests** - Validation end-to-end

---

## Ports des Services

| Service | Port |
|---------|------|
| auth-service | 8081 |
| card-service | 8082 |
| account-service | 8083 |
| transaction-service | 8084 |
| partner-service | 8085 |
| **notification-service** | **8086** |
| Kafka | 29092 (host) / 9092 (docker) |
| Zookeeper | 2181 |
