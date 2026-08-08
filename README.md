# Janus

English | [中文](./README.zh-CN.md)

> A self-hosted, **OpenAI-compatible multi-model LLM gateway** — multi-channel routing, failover, token-quota billing, and rate limiting in one reactive Java service.

Janus sits between your clients and your LLM backends (OpenAI, Zhipu GLM, local mlx-lm / Ollama / vLLM, …). Clients speak a single OpenAI-compatible API; Janus takes care of **routing across channels, automatic failover, token-quota billing, and rate limiting** — giving you resiliency, cost control, and fair usage without changing a line of client code.

---

## Why

Running LLMs in production raises problems a plain proxy can't solve:

- **One backend down shouldn't take you down.** You want multiple channels per model and automatic failover.
- **Cost must be controlled per tenant.** You need token-level quota that survives concurrency and crashes.
- **A single noisy client must not starve others.** You need rate limiting that protects both the gateway and the upstream.

Janus is built as a **production-grade gateway** to solve exactly these — per-channel circuit breakers, Redis-Lua quota consistency that survives concurrency and crashes, sliding-window rate limiting, and backpressure-aware SSE proxying.

---

## Features

| Area | What you get |
|---|---|
| **Routing & Failover** | Per-model channel selection, weighted ordering, Resilience4j circuit breaker per channel, failover **before first byte** for streaming |
| **Quota Billing** | Redis-Lua atomic `reserve → settle → refund`, symmetric settlement (refund over-reservation / charge under-reservation), idempotent via `request_id`, crash recovery + minute-level reconciliation |
| **Rate Limiting** | Sliding-window limiter (Redis ZSET) at **API-key** and **model** level, evaluated **before** quota reservation so rejected requests cost nothing |
| **Auth** | Virtual API keys (`sk-janus-…`), SHA256-hashed at rest, Redis-cached with **cache-penetration protection** |
| **Streaming** | Full SSE pass-through with backpressure; usage accumulated locally and merged with upstream `stream_options.include_usage` |
| **API** | Drop-in OpenAI-compatible: `POST /v1/chat/completions` (stream & non-stream) |

> IP-level rate limiting and TLS termination are intentionally left to a fronting traffic gateway (Nginx / cloud LB / WAF). See [Deployment layering](#deployment-layering).

---

## Architecture

```
Client (OpenAI-compatible)
    │  POST /v1/chat/completions   Authorization: Bearer sk-janus-…
    ▼
┌──────────────────────────────────────────────┐
│  Janus (Spring WebFlux, :8090)               │
│  Reactive filter chain:                      │
│    Auth → RateLimit → QuotaPreCheck          │
│      → Router → UpstreamProxy → Metering     │
│                                              │
│  ChannelRouter   weighted + circuit breaker  │
│  UpstreamProxy   SSE pass-through + failover │
└──┬───────────────┬──────────────────────────┘
   │ Redis         │ MySQL
   │ quota / rate  │ channel / token /
   │ limit / auth  │ usage_log / reserved_record
   ▼               ▼
              Upstream LLM
              (OpenAI / GLM / mlx / ollama / vLLM …)
```

**Request lifecycle:**

1. **Auth** — verify the API key (Redis-cached, penetration-proof).
2. **RateLimit** — sliding-window check at key + model level. Over-limit → `429`, nothing else happens (no quota spent, no DB write).
3. **QuotaPreCheck** — atomically reserve `est_prompt + min(max_tokens, cap)` tokens via Lua; persist an in-flight `reserved_record` (the crash-recovery source of truth).
4. **Router → UpstreamProxy** — pick a healthy channel, forward, fail over before first byte if needed; for streaming, accumulate usage from chunks.
5. **Metering** — symmetric settle: `delta = reserved − actual` (positive = refund, negative = charge back); idempotent; append `usage_log`.

Background jobs: `QuotaBootstrap` (warmup/rebuild Redis from MySQL), `QuotaRecoverJob` (refund stale reservations), `QuotaReconcileJob` (correct drift).

---

## Quick Start

### Prerequisites

- JDK 21, Docker

### 1. Start MySQL + Redis

```bash
export DATA_DIR="$HOME/.janus-data"   # mounted volume for persistent data
docker compose up -d
```

### 2. Initialize the schema

```bash
for f in .sql/*.sql; do
  docker exec -i janus-mysql mysql -uroot -proot < "$f"
done
```

### 3. Create an API key

```bash
./scripts/gen-token.sh mykey "glm-5.1" 1000000
# → prints a plaintext key (save it!) + a ready-to-run INSERT
# run the INSERT against the janus DB
```

### 4. Configure an upstream channel

Insert a row into the `channel` table pointing to an OpenAI-compatible backend, e.g. a local `mlx-lm server` on `http://127.0.0.1:28000/v1`, or a cloud provider. Review `.sql/channel.sql` for the shape.

### 5. Review config

Edit `src/main/resources/application.yml` — DB/Redis connection, upstream defaults, quota & rate-limit tuning.

### 6. Run

```bash
./mvnw spring-boot:run
```

### 7. Call it

```bash
curl http://localhost:8090/v1/chat/completions \
  -H "Authorization: Bearer sk-janus-…" \
  -H "Content-Type: application/json" \
  -d '{
    "model": "glm-5.1",
    "messages": [{"role":"user","content":"hello"}]
  }'
```

Add `"stream": true` for SSE.

---

## Configuration

Key entries in `application.yml`:

```yaml
janus:
  quota:
    credit-limit: 2000          # allowed overdraft (tokens)
    recover-stale-minutes: 10   # reservation timeout for recovery
    reconcile-threshold: 100    # drift threshold for reconciliation
    model-completion-cap:
      default: 2048             # per-model completion reservation cap
  ratelimit:
    window-seconds: 60
    token-limit: 100            # max requests / window per API key
    model-limit: 50             # max requests / window per model
```

Redis keys: `quota:{tokenId}` (balance), `rl:token:{id}` / `rl:model:{name}` (sliding windows), `token:{hash}` (auth cache).

---

## API

OpenAI-compatible. Errors return `{code, message, data, requestId}` with HTTP-mapped status codes (`402` quota exceeded, `429` rate limited, `502` all channels failed, `503` all circuits open, …).

---

## Deployment layering

Janus is a **business gateway**. For production, put a **traffic gateway** (Nginx / cloud LB / WAF) in front of it:

```
Client → Nginx (TLS, load balance, IP/conn limiting, DDoS) → Janus × N → upstreams
```

Janus is stateless (all state in MySQL/Redis), so it scales horizontally behind the LB. IP-level rate limiting belongs at the traffic gateway; Janus focuses on key/model-level (business) rate limiting.

---

## Roadmap

| Phase | Status | Scope |
|---|---|---|
| 0 | ✅ Done | OpenAI-compatible passthrough + SSE |
| 1 | ✅ Done | Multi-channel routing + failover + circuit breaker |
| 2 | ✅ Done | API key + quota billing (Redis-Lua consistency, crash recovery) |
| 3 | ✅ Done | Multi-level rate limiting (sliding window) |
| 4 | 🔜 Planned | Usage audit via Kafka (decouple high-frequency writes from the hot path) |
| 5 | 🔜 Planned | Multi-provider adapters (Claude/Gemini) + admin console |

---

## Tech Stack

Java 21 · Spring Boot 3.5 (WebFlux) · MyBatis-Plus · MySQL 8 · Redis 7 (Lettuce + Lua) · Resilience4j · jtokkit · Maven

---

## Project Structure

```
src/main/java/com/marsdev/janus/
├── controller/     RelayController (OpenAI-compatible endpoints)
├── filter/         AuthFilter, RateLimitFilter, QuotaPreCheckFilter, MeteringFilter
├── channel/        ChannelRouter (weighted routing + circuit breaker)
├── reply/          UpstreamProxy (SSE + failover), UsageParser
├── quota/          QuotaService (Lua), Bootstrap, RecoverJob, ReconcileJob, estimator
├── ratelimit/      RateLimitService (sliding window)
├── entity / mapper / model / common
src/main/resources/
├── application.yml
└── lua/            quota_pre_consume.lua, quota_adjust.lua, ratelimit.lua
.sql/               schema + sample data
scripts/            gen-token.sh (generate API keys)
```

---

## License

[MIT](./LICENSE)
