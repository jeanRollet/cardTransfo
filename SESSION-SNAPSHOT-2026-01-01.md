# Session Snapshot - 2026-01-01

## État Actuel

### GitHub Repository
**URL** : https://github.com/jeanRollet/cardTransfo

### Services Implémentés ✅

| Service | Port | Équivalent CICS | Status |
|---------|------|-----------------|--------|
| auth-service | 8081 | COSGN00C (Login) | ✅ Complet |
| card-service | 8082 | COCRDLIC (Cards) | ✅ Complet |
| account-service | 8083 | COACTVWC (Accounts) | ✅ Complet |
| transaction-service | 8084 | COTRN00C (History) | ✅ Complet |
| partner-service | 8085 | CICS Web Services | ✅ Complet |
| **notification-service** | **8086** | **MQ Series** | ⏳ En cours |

### Infrastructure ✅
- PostgreSQL 16 avec schéma complet
- Redis pour rate limiting + sessions
- Docker Compose fonctionnel
- Frontend React (port 3000)

---

## En Cours : Kafka Event-Driven Architecture

### Plan : `/home/ubuntu/.claude/plans/parallel-waddling-cocoa.md`
### Copie dans repo : `PLAN-KAFKA-EVENT-DRIVEN.md`

### Progression Todo List

1. ⏳ **Ajouter Kafka/Zookeeper au docker-compose.yml** (commencé - volumes ajoutés)
2. ⬚ Ajouter tables outbox et webhook au schéma PostgreSQL
3. ⬚ Créer Event DTOs dans le module shared
4. ⬚ Créer notification-service (consumers, webhook delivery)
5. ⬚ Ajouter Kafka producer au transaction-service
6. ⬚ Ajouter Kafka producer au card-service
7. ⬚ Ajouter Kafka producer au account-service
8. ⬚ Mettre à jour le parent POM

### Décisions Prises
- **Transactions** : Endpoint de simulation (pas de POST réel)
- **Sécurité Webhook** : Header simple `X-Webhook-Secret`
- **Pattern Outbox = UOW CICS** : Atomicité garantie

---

## Pour Reprendre

```bash
cd /home/ubuntu/git/cardTransfo
git pull origin main
```

### Commande pour continuer l'implémentation Kafka :
> "Continue l'implémentation Kafka Event-Driven selon le plan PLAN-KAFKA-EVENT-DRIVEN.md"

---

## Fichiers Clés

```
/home/ubuntu/git/cardTransfo/
├── PLAN.md                        # Partner Integration (fait)
├── PLAN-KAFKA-EVENT-DRIVEN.md     # Kafka (à faire)
├── appli/
│   ├── pom.xml                    # Parent POM
│   ├── auth-service/              # ✅
│   ├── card-service/              # ✅
│   ├── account-service/           # ✅
│   ├── transaction-service/       # ✅
│   ├── partner-service/           # ✅
│   ├── shared/                    # À enrichir avec Event DTOs
│   └── notification-service/      # À CRÉER
├── frontend/                      # ✅ React
└── infra/
    ├── docker/docker-compose.yml  # À modifier pour Kafka
    └── config/postgresql/
        ├── init-db.sql            # À modifier pour outbox tables
        └── seed-data.sql          # ✅
```

---

## Credentials Configurés

- **GitHub** : Token dans `~/.git-credentials`
- **PostgreSQL** : carddemo / carddemo123
- **Redis** : carddemo123

---

## Architecture Cible Kafka

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
