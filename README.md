# Distributed Rate Limiter

A production-grade, multi-tenant rate limiting service built as a shared infrastructure primitive the same pattern used by Stripe, Cloudflare, and AWS API Gateway.

Instead of every service implementing its own rate limiting logic, they delegate to this centralized service via a single REST call. The result is globally consistent, atomic rate limiting across any number of service instances.

---

## The Problem It Solves

When you run multiple instances of a service, local in-process rate limiters break  each instance has its own counter. A user hitting three instances gets three times the allowed limit.

This service solves that by centralizing all counters in Redis. Every instance shares the same state. The count is always globally accurate.

---

## How It Works

Any service that wants to rate limit a user makes one call:

```bash
POST /api/v1/rate-limiter/check
X-API-Key: sk_socials_abc123

{
  "tenantId": "acme-corp",
  "userId":   "user-123",
  "action":   "ai_generate",
  "cost":     1
}
```

The response comes back in milliseconds:

```json
{
  "allowed":   true,
  "remaining": 4,
  "resetAt":   0,
  "reason":    "ok"
}
```

The calling service either proceeds or returns a `429 Too Many Requests` to its own client. That's it.

---

## Architecture

```
Any Service (Socials, Ads, Pages)
        │
        │  POST /check
        ▼
  Rate Limiter Service
        │
        ├──▶ Caffeine Cache ──▶ PostgreSQL (tenant plan config)
        │
        └──▶ Redis Lua Script (atomic counter — sliding window or token bucket)
                │
                ├── allowed → return 200
                └── denied  → return 429
```

---

## Key Technical Decisions

### Atomic Lua Scripts on Redis
Race conditions are the core problem in distributed rate limiting. Two simultaneous requests read the same count, both see "under limit," both proceed over-counting happens.

The fix: Lua scripts execute entirely on the Redis server in a single atomic operation. No client-side read-modify-write. No race conditions. No distributed locks needed.

### Two Algorithms via Strategy Pattern
Each tenant's plan defines which algorithm applies:

**Sliding Window**:  accurate, fair. Counts requests in a rolling time window. Best for strict quota enforcement.

**Token Bucket**: generous with bursts. Users can spike up to their capacity then recover at the refill rate. Best for APIs where short bursts are legitimate.

Adding a third algorithm requires one new class. Zero changes to existing code.

### Multi-Tenant Config with Caffeine Cache
Plan limits live in PostgreSQL. Fetching from the database on every request would kill performance at scale.

Solution: Caffeine in-memory cache with a 30-second TTL. First request per tenant hits the DB. Every subsequent request returns in nanoseconds from memory. When a tenant upgrades their plan, a Kafka event evicts the stale cache entry — new limits apply within milliseconds.

### Graceful Degradation
Redis going down doesn't crash the service. Each tenant has a configurable `fail_behavior`:
- `fail_open` → allow all requests (prefer availability)
- `fail_closed` → deny all requests (prefer safety)

The right choice depends on what's being protected.

---

## Tech Stack

| Layer | Technology | Why |
|-------|-----------|-----|
| API | Spring Boot 3.x | Production-grade REST, dependency injection, actuator |
| Rate limiting storage | Redis + Lua | Atomic distributed counters, sub-millisecond reads |
| Config storage | PostgreSQL + Flyway | Durable tenant config, versioned schema migrations |
| Config cache | Caffeine | Nanosecond in-process cache, avoids DB on hot path |
| Plan change events | Kafka | Decoupled plan upgrades, cache invalidation at scale |
| Metrics | Micrometer + Prometheus + Grafana | Real-time observability per tenant and algorithm |
| Auth | API Key filter | Only trusted services can call the rate limiter |

---

## Endpoints

| Method | Path | Description |
|--------|------|-------------|
| `POST` | `/api/v1/rate-limiter/check` | Check and consume rate limit |
| `POST` | `/api/v1/tenants` | Create a new tenant |
| `GET` | `/api/v1/tenants/{tenantId}` | Get tenant config |
| `PUT` | `/api/v1/tenants/{tenantId}/plan` | Upgrade tenant plan |
| `POST` | `/api/v1/plans/upgrade` | Trigger plan upgrade via Kafka |
| `GET` | `/actuator/health` | Health check |
| `GET` | `/actuator/prometheus` | Metrics scrape endpoint |

All endpoints (except actuator) require `X-API-Key` header.

---

## Plans

| Plan | Capacity | Refill Rate | Algorithm |
|------|----------|-------------|-----------|
| free | 5 | 0.1/s | Sliding Window |
| pro | 50 | 1/s | Token Bucket |
| pro_plus | 200 | 5/s | Token Bucket |

---

## Running Locally

**Prerequisites:** Docker, Java 21, Maven

```bash
# Clone the repo
git clone https://github.com/your-username/distributed-rate-limiter
cd distributed-rate-limiter

# Copy environment template
cp .env.example local.properties
# Fill in your values in local.properties

# Start infrastructure
docker compose up -d

# Run the app
mvn spring-boot:run
```

Services will be available at:
- API: `http://localhost:8080`
- Grafana: `http://localhost:3000` (admin/admin)
- Prometheus: `http://localhost:9090`

---

## What I Learned Building This

- **Atomic distributed operations** — why Lua scripts on Redis eliminate race conditions without distributed locks
- **Multi-tenancy design** — isolating counters per tenant while sharing infrastructure
- **Graceful degradation** — systems that make intelligent decisions under partial failure
- **The strategy pattern in practice** — swapping algorithms at runtime based on configuration
- **Observability from day one** — Micrometer counters with tags that let you slice data any way you need in Grafana
- **Event-driven config updates** — Kafka delivering plan changes to invalidate stale cache entries without downtime
