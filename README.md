# Powerbank Sharing Service — MVP

A monorepo microservices backend for a powerbank sharing (sharing) startup built for the Anor Accelerator technical assignment.

## Stack

- **Java 17** + **Spring Boot 3.3**
- **PostgreSQL 16** (one DB per service)
- **Apache Kafka** (event-driven async flow)
- **Keycloak 24** (OAuth2 / JWT auth)
- **Kong Gateway 3.6** (DB-less mode, JWT plugin)
- **Lombok** (DTOs + entities)
- **Spring Data JPA** (repositories)
- **Liquibase** (migrations — no `ddl-auto=create`)
- **Docker Compose** (full stack)

## Services

| Service | Port | DB Port | Role |
|---|---|---|---|
| user-service | 8081 | 5433 | Auth, OTP, Keycloak integration |
| payment-service | 8082 | 5434 | Cards, payments, Kafka events |
| station-service | 8083 | 5435 | Stations, slots, IoT simulation |
| rental-service | 8084 | 5436 | Rental FSM orchestrator |
| Kong Gateway | 80 | — | Auth + routing |
| Keycloak | 8080 | — | OAuth2 server |
| Kafka | 9092 | — | Event bus |

## Quick Start

### Option 1: Docker Compose (Recommended)

```bash
git clone <repo-url>
cd java_test
docker-compose up --build
```

All services start automatically. Keycloak imports the `powerbank` realm on first boot.

### Option 2: Run Services Individually

Prerequisites: PostgreSQL, Kafka running locally.

```bash
# Terminal 1: User Service
cd user-service && mvn spring-boot:run

# Terminal 2: Payment Service
cd payment-service && mvn spring-boot:run

# Terminal 3: Station Service
cd station-service && mvn spring-boot:run

# Terminal 4: Rental Service
cd rental-service && mvn spring-boot:run
```

## API Endpoints

All public endpoints are accessible via Kong at `http://localhost:80`.

### Auth (User Service)
```
POST /auth/phone       — Request OTP
POST /auth/verify      — Verify OTP, get JWT
POST /v1/auth/refresh  — Refresh token
GET  /v1/me            — My profile (JWT required)
```

### Stations
```
GET /v1/stations?lat=41.3&lon=69.2  — Nearest stations
GET /v1/stations/{id}                — Station detail
```

### Rental Flow
```
POST /api/v1/rentals              — Create rental (start flow)
GET  /api/v1/rentals/{id}/status  — Poll status (every 1s)
GET  /api/v1/rentals/history      — My rental history
POST /api/v1/rentals/finish       — Return powerbank
```

### Payments
```
POST   /v1/cards       — Add card
GET    /v1/cards       — My cards
DELETE /v1/cards/{id}  — Remove card
POST   /v1/payments    — Create payment
DELETE /v1/payments/{id} — Cancel payment
```

## Rental FSM States

```
WAITING → ACQUIRING_LOCK → PAYMENT_PROCESSING → EJECTING → IN_THE_LEASE
                                                                    ↓
                                                              FINISHED
Any state → FAILED (on error)
```

## Kafka Topics

| Topic | Producer | Consumer |
|---|---|---|
| `acquire-cabinet-lock-event` | rental-service | station-service |
| `acquire-cabinet-lock-result` | station-service | rental-service |
| `payment-request` | rental-service | payment-service |
| `payment-events` | payment-service | rental-service |
| `eject-powerbank-event` | rental-service | station-service |
| `eject-powerbank-result` | station-service | rental-service |

## Architecture Decisions

See [DECISIONS.md](./DECISIONS.md) for full rationale on:
- UUID vs BIGSERIAL
- NUMERIC vs DOUBLE for money
- TIMESTAMPTZ vs TIMESTAMP
- Kafka key choice and ordering
- Idempotency conflict handling
- Outbox pattern discussion

## Example: Full Rental Flow

```bash
# 1. Request OTP
curl -X POST http://localhost/auth/phone \
  -H "Content-Type: application/json" \
  -d '{"phone": "+998901234567"}'

# 2. Verify OTP (check logs for code in dev mode)
curl -X POST http://localhost/auth/verify \
  -H "Content-Type: application/json" \
  -d '{"phone": "+998901234567", "code": "123456"}'
# → {"accessToken": "...", "refreshToken": "..."}

# 3. Add a card
curl -X POST http://localhost/v1/cards \
  -H "Authorization: Bearer <token>" \
  -H "Content-Type: application/json" \
  -d '{"cardNumber":"4111111111111111","cardHolder":"JOHN DOE","initialBalance":100.00}'

# 4. Create a rental
curl -X POST http://localhost/api/v1/rentals \
  -H "Authorization: Bearer <token>" \
  -H "Content-Type: application/json" \
  -d '{"stationId":"a1000000-0000-0000-0000-000000000001","cardId":"<card-id>","idempotencyKey":"my-key-1"}'
# → {"id":"<rental-id>","status":"ACQUIRING_LOCK",...}

# 5. Poll status every second
curl http://localhost/api/v1/rentals/<rental-id>/status \
  -H "Authorization: Bearer <token>"
# Status progresses: ACQUIRING_LOCK → PAYMENT_PROCESSING → EJECTING → IN_THE_LEASE ✅

# 6. Return powerbank
curl -X POST http://localhost/api/v1/rentals/finish \
  -H "Authorization: Bearer <token>" \
  -H "Content-Type: application/json" \
  -d '{"rentalId":"<rental-id>"}'
```
# java_assignment
# java_assignment
