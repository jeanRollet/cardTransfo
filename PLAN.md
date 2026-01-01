# Plan d'Implémentation - Intégration Partenaire (API Gateway)

> **STATUS: IMPLÉMENTÉ** - Le partner-service est complet et prêt pour déploiement.

## Contexte

L'intégration partenaire représente un cas typique de transformation mainframe : remplacer les connexions **CICS Web Services / MQ Series** par une **API REST sécurisée** accessible aux partenaires externes (fintechs, commerçants, processeurs de paiement).

## Architecture Cible

```
┌─────────────────┐     ┌──────────────────────┐     ┌─────────────────────┐
│   Partenaire    │────▶│   Partner Gateway    │────▶│   CardDemo APIs     │
│   (Fintech)     │     │   (api-gateway)      │     │   (microservices)   │
│                 │     │   - API Keys         │     │                     │
│  X-API-Key:xxx  │     │   - Rate Limiting    │     │  - account-service  │
│                 │     │   - Quota Management │     │  - card-service     │
└─────────────────┘     │   - Request Logging  │     │  - transaction-svc  │
                        └──────────────────────┘     └─────────────────────┘
```

## Équivalence CICS/MQ → Cloud Native

| Mainframe                     | Cloud Native                    |
|-------------------------------|----------------------------------|
| CICS Web Services (SOAP)      | REST API + JSON                  |
| MQ Series (queues)            | HTTP + API Gateway               |
| RACF Resource Security        | API Key + Scopes                 |
| CICS Transaction Limits       | Rate Limiting                    |
| SMF Records                   | Request Logging (audit)          |

---

## Composants à Implémenter

### 1. Nouveau Service : `partner-service` (Port 8085)

**Responsabilités :**
- Gestion des partenaires (CRUD)
- Génération et validation des API Keys
- Gestion des quotas et rate limits
- Logging des requêtes API partenaires

**Endpoints :**
```
POST   /api/v1/partners                    - Enregistrer un nouveau partenaire (admin)
GET    /api/v1/partners                    - Lister les partenaires (admin)
GET    /api/v1/partners/{partnerId}        - Détails d'un partenaire
PUT    /api/v1/partners/{partnerId}        - Mettre à jour un partenaire
DELETE /api/v1/partners/{partnerId}        - Désactiver un partenaire

POST   /api/v1/partners/{partnerId}/keys   - Générer une nouvelle API Key
GET    /api/v1/partners/{partnerId}/keys   - Lister les API Keys (masquées)
DELETE /api/v1/partners/{partnerId}/keys/{keyId} - Révoquer une API Key

GET    /api/v1/partners/{partnerId}/usage  - Consulter l'utilisation/quotas
```

### 2. Tables PostgreSQL

```sql
-- Table des partenaires
CREATE TABLE partners (
    partner_id SERIAL PRIMARY KEY,
    partner_name VARCHAR(100) NOT NULL,
    partner_type VARCHAR(20) NOT NULL,        -- FINTECH, MERCHANT, PROCESSOR
    contact_email VARCHAR(100) NOT NULL,
    webhook_url VARCHAR(255),                 -- URL callback (optionnel)
    allowed_scopes TEXT[],                    -- ['accounts:read', 'transactions:read']
    rate_limit_per_minute INTEGER DEFAULT 60,
    daily_quota INTEGER DEFAULT 10000,
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Table des API Keys
CREATE TABLE partner_api_keys (
    key_id SERIAL PRIMARY KEY,
    partner_id INTEGER NOT NULL REFERENCES partners(partner_id),
    api_key_hash VARCHAR(256) NOT NULL,       -- SHA-256 hash (pas de stockage en clair)
    key_prefix VARCHAR(8) NOT NULL,           -- "pk_live_" pour identification
    description VARCHAR(100),
    scopes TEXT[],                            -- Permissions de cette clé
    expires_at TIMESTAMP,
    last_used_at TIMESTAMP,
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Table d'audit des requêtes API
CREATE TABLE partner_api_logs (
    log_id BIGSERIAL PRIMARY KEY,
    partner_id INTEGER REFERENCES partners(partner_id),
    api_key_prefix VARCHAR(8),
    endpoint VARCHAR(255),
    method VARCHAR(10),
    status_code INTEGER,
    response_time_ms INTEGER,
    ip_address INET,
    user_agent VARCHAR(255),
    request_timestamp TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Table des quotas journaliers
CREATE TABLE partner_daily_usage (
    usage_id SERIAL PRIMARY KEY,
    partner_id INTEGER REFERENCES partners(partner_id),
    usage_date DATE NOT NULL,
    request_count INTEGER DEFAULT 0,
    UNIQUE(partner_id, usage_date)
);
```

### 3. Filtre d'Authentification API Key

**Nouveau filtre Spring Security** dans chaque service exposé aux partenaires :

```java
// ApiKeyAuthenticationFilter.java
// Intercepte les requêtes avec header "X-API-Key"
// Valide la clé via appel au partner-service
// Extrait les scopes et le partner_id
// Rate limiting via Redis (sliding window)
```

**Header requis :**
```
X-API-Key: pk_live_xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx
```

### 4. Endpoints Partenaires (Lecture seule)

Exposer une version "partenaire" des APIs existantes avec restrictions :

```
GET /partner/v1/accounts/{accountId}           - Compte (si accord partenaire)
GET /partner/v1/accounts/{accountId}/balance   - Solde simplifié
GET /partner/v1/transactions/{accountId}       - Historique transactions
GET /partner/v1/cards/{accountId}/status       - Statut des cartes (masqué)
```

**Différences vs API interne :**
- Données sensibles masquées (numéros complets de carte)
- Pagination obligatoire
- Champs limités selon les scopes

### 5. Rate Limiting (Redis)

Utiliser Redis pour le rate limiting (sliding window) :

```
partner:{partnerId}:minute:{timestamp} → counter
partner:{partnerId}:day:{date} → counter
```

**Réponse en cas de dépassement :**
```json
HTTP 429 Too Many Requests
{
  "error": "RATE_LIMIT_EXCEEDED",
  "message": "Rate limit exceeded. Try again in 45 seconds.",
  "retryAfter": 45,
  "limit": 60,
  "remaining": 0,
  "resetAt": "2025-12-31T10:45:00Z"
}
```

---

## Étapes d'Implémentation

### Phase 1 : Infrastructure Base
1. Créer le module `partner-service` (Spring Boot)
2. Ajouter les tables PostgreSQL (init-db.sql)
3. Configurer Redis pour rate limiting
4. Créer les entités JPA (Partner, ApiKey, ApiLog)

### Phase 2 : Gestion des Partenaires
5. Implémenter le CRUD des partenaires
6. Implémenter la génération d'API Keys (SHA-256 hash)
7. Créer l'endpoint de validation de clé (`/internal/validate-key`)
8. Ajouter les données de test (seed-data.sql)

### Phase 3 : Authentification & Sécurité
9. Créer le filtre `ApiKeyAuthenticationFilter`
10. Implémenter le rate limiting Redis
11. Ajouter le logging des requêtes API

### Phase 4 : Endpoints Partenaires
12. Créer les endpoints `/partner/v1/*` dans les services existants
13. Implémenter le masquage des données sensibles
14. Ajouter la validation des scopes

### Phase 5 : Documentation & Test
15. Documenter l'API partenaire (OpenAPI/Swagger)
16. Créer un partenaire de test avec API key
17. Tester le flux complet

---

## Données de Test

```sql
-- Partenaire de test : Fintech ABC
INSERT INTO partners (partner_name, partner_type, contact_email, allowed_scopes, rate_limit_per_minute, daily_quota)
VALUES ('Fintech ABC', 'FINTECH', 'api@fintechabc.com',
        ARRAY['accounts:read', 'transactions:read'], 60, 10000);

-- API Key de test (préfixe visible, le reste hashé)
-- Clé complète : pk_test_abc123xyz456def789ghi012jkl345
INSERT INTO partner_api_keys (partner_id, api_key_hash, key_prefix, description, scopes)
VALUES (1, 'sha256_hash_here', 'pk_test_', 'Test API Key',
        ARRAY['accounts:read', 'transactions:read']);
```

---

## Tests de Validation

```bash
# 1. Requête partenaire valide
curl -H "X-API-Key: pk_test_abc123xyz456def789ghi012jkl345" \
     http://localhost:8085/partner/v1/accounts/1000000001

# 2. Test rate limiting (61 requêtes en 1 minute)
for i in {1..61}; do
  curl -s -o /dev/null -w "%{http_code}\n" \
       -H "X-API-Key: pk_test_..." \
       http://localhost:8085/partner/v1/accounts/1000000001
done
# La 61ème requête retourne 429

# 3. Test scope invalide
curl -H "X-API-Key: pk_test_..." \
     http://localhost:8085/partner/v1/cards/1000000001/block
# Retourne 403 Forbidden (scope cards:write non autorisé)
```

---

## Résultat Attendu

Après implémentation :
- Un partenaire peut s'authentifier avec une API Key
- Les requêtes sont limitées (60/min, 10000/jour)
- Toutes les requêtes sont loggées pour audit
- Les données sensibles sont masquées
- La documentation OpenAPI est disponible pour les partenaires

Cette implémentation démontre la transformation typique des **CICS Web Services** vers une **API REST moderne** avec authentification machine-to-machine.

---

## Fichiers Créés

### Backend - partner-service (Port 8085)
```
appli/partner-service/
├── pom.xml
├── Dockerfile
└── src/main/
    ├── java/com/carddemo/partner/
    │   ├── PartnerServiceApplication.java
    │   ├── config/
    │   │   ├── OpenApiConfig.java
    │   │   ├── RedisConfig.java
    │   │   └── SecurityConfig.java
    │   ├── controller/
    │   │   ├── InternalController.java
    │   │   ├── PartnerApiController.java
    │   │   └── PartnerController.java
    │   ├── dto/
    │   │   ├── ApiKeyRequest.java
    │   │   ├── ApiKeyResponse.java
    │   │   ├── ApiKeyValidationResponse.java
    │   │   ├── PartnerRequest.java
    │   │   ├── PartnerResponse.java
    │   │   └── UsageResponse.java
    │   ├── entity/
    │   │   ├── Partner.java
    │   │   ├── PartnerApiKey.java
    │   │   ├── PartnerApiLog.java
    │   │   └── PartnerDailyUsage.java
    │   ├── exception/
    │   │   ├── GlobalExceptionHandler.java
    │   │   └── PartnerException.java
    │   ├── repository/
    │   │   ├── PartnerApiKeyRepository.java
    │   │   ├── PartnerApiLogRepository.java
    │   │   ├── PartnerDailyUsageRepository.java
    │   │   └── PartnerRepository.java
    │   └── service/
    │       ├── ApiKeyService.java
    │       ├── PartnerService.java
    │       ├── RateLimitService.java
    │       └── UsageService.java
    └── resources/
        └── application.yml
```

### Base de données - Tables ajoutées
- `partners` - Registre des partenaires
- `partner_api_keys` - Clés API (hashées)
- `partner_api_logs` - Audit des requêtes
- `partner_daily_usage` - Quotas journaliers

### Configuration
- `infra/docker/docker-compose.yml` - Service partner-service ajouté
- `infra/config/postgresql/init-db.sql` - Tables partenaires ajoutées
- `infra/config/postgresql/seed-data.sql` - Données de test ajoutées

---

## Démarrage Rapide

```bash
# Démarrer la stack complète
cd infra/docker
docker-compose up -d

# Créer une API Key pour le partenaire test
curl -X POST http://localhost:8085/api/v1/partners/1/keys \
     -H "Content-Type: application/json" \
     -d '{"description": "Test Key"}'
# Sauvegarder la clé retournée (ex: pk_live_abc123...)

# Tester l'API partenaire
curl -H "X-API-Key: pk_live_abc123..." \
     http://localhost:8085/partner/v1/accounts/1000000001

# Documentation Swagger
open http://localhost:8085/swagger-ui.html
```
