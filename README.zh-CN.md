# Janus

> 自托管的、**OpenAI 兼容的多模型 LLM 网关** —— 在一个响应式 Java 服务里集成多渠道路由、故障转移、按 token 配额计费与限流。

[English](./README.md) | 中文

Janus 部署在你的客户端和 LLM 后端（OpenAI、智谱 GLM、本地 mlx-lm / Ollama / vLLM ……）之间。客户端只需对接一套 OpenAI 兼容 API，Janus 负责跨渠道路由、自动故障转移、token 配额计费与限流——让你无需改动客户端代码，就获得**高可用、成本可控、用量公平**。

---

## 为什么要做

在生产环境跑 LLM，会冒出普通代理解决不了的问题：

- **一家后端挂了不能把你整垮。** 你希望每个模型背后挂多个渠道，并能自动故障转移。
- **成本必须按租户控制。** 你需要 token 级配额，且扛得住并发和进程崩溃。
- **单个吵闹的客户端不能饿死其他人。** 你需要限流，同时保护网关自身和上游。

Janus 定位为**生产级网关**，正是为解决这三件事而生——按渠道断路器、抗并发与崩溃的 Redis Lua 配额一致性、滑动窗口限流、带背压的 SSE 代理。

---

## 核心特性

| 领域 | 能力 |
|---|---|
| **路由与故障转移** | 按模型选渠道、加权排序、每个渠道独立 Resilience4j 断路器、流式**首字节前**可故障转移 |
| **配额计费** | Redis Lua 原子「预扣 → 结算 → 退还」，对称结算（多扣退还 / 少扣补扣），`request_id` 幂等，崩溃恢复 + 分钟级对账 |
| **限流** | 滑动窗口限流（Redis ZSET），支持 **API Key** 与 **模型** 两级，且在**预扣之前**判断——被拒请求零成本 |
| **鉴权** | 虚拟 API Key（`sk-janus-…`），库内只存 SHA256，Redis 缓存 + **防穿透** |
| **流式** | 完整 SSE 透传（含背压）；本地累加 usage，并与上游 `stream_options.include_usage` 合并 |
| **API** | 开箱即用的 OpenAI 兼容：`POST /v1/chat/completions`（流式 + 非流式） |

> IP 级限流与 TLS 卸载有意留给前置的流量网关（Nginx / 云 LB / WAF）。见[部署分层](#部署分层)。

---

## 架构

```
Client (OpenAI 兼容)
    │  POST /v1/chat/completions   Authorization: Bearer sk-janus-…
    ▼
┌──────────────────────────────────────────────┐
│  Janus (Spring WebFlux, :8090)               │
│  响应式 filter 链：                          │
│    Auth → RateLimit → QuotaPreCheck          │
│      → Router → UpstreamProxy → Metering     │
│                                              │
│  ChannelRouter   加权 + 断路器               │
│  UpstreamProxy   SSE 透传 + 故障转移         │
└──┬───────────────┬──────────────────────────┘
   │ Redis         │ MySQL
   │ 配额 / 限流   │ channel / token /
   │ / 鉴权缓存    │ usage_log / reserved_record
   ▼               ▼
              上游 LLM
              (OpenAI / GLM / mlx / ollama / vLLM …)
```

**请求生命周期：**

1. **Auth**——校验 API Key（Redis 缓存，防穿透）。
2. **RateLimit**——key + model 级滑动窗口检查。超限 → `429`，什么都不做（不扣额度、不写库）。
3. **QuotaPreCheck**——用 Lua 原子预扣 `est_prompt + min(max_tokens, cap)` token；同步落一条在途 `reserved_record`（崩溃恢复的真相源）。
4. **Router → UpstreamProxy**——选健康渠道，转发，首字节前失败则故障转移；流式则累加 chunk 的 usage。
5. **Metering**——对称结算：`delta = reserved − actual`（正数退还，负数补扣）；幂等；追加 `usage_log`。

后台 Job：`QuotaBootstrap`（预热/从 MySQL 重建 Redis）、`QuotaRecoverJob`（退还超时未结算的预扣）、`QuotaReconcileJob`（修正偏差）。

---

## 快速开始

### 前置

- JDK 21、Docker

### 1. 启动 MySQL + Redis

```bash
export DATA_DIR="$HOME/.janus-data"   # 持久化数据的挂载目录
docker compose up -d
```

### 2. 初始化表结构

```bash
for f in .sql/*.sql; do
  docker exec -i janus-mysql mysql -uroot -proot < "$f"
done
```

### 3. 创建 API Key

```bash
./scripts/gen-token.sh mykey "glm-5.1" 1000000
# → 打印明文 key（请保存！）+ 一条可直接执行的 INSERT
# 把这条 INSERT 跑进 janus 库
```

### 4. 配置上游渠道

向 `channel` 表插入一条指向 OpenAI 兼容后端的记录，例如本地 `mlx-lm server`（`http://127.0.0.1:28000/v1`）或云厂商。字段结构见 `.sql/channel.sql`。

### 5. 检查配置

编辑 `src/main/resources/application.yml`——数据库/Redis 连接、上游默认、配额与限流参数。

### 6. 启动

```bash
./mvnw spring-boot:run
```

### 7. 调用

```bash
curl http://localhost:8090/v1/chat/completions \
  -H "Authorization: Bearer sk-janus-…" \
  -H "Content-Type: application/json" \
  -d '{
    "model": "glm-5.1",
    "messages": [{"role":"user","content":"你好"}]
  }'
```

加 `"stream": true` 走 SSE。

---

## 配置

`application.yml` 关键项：

```yaml
janus:
  quota:
    credit-limit: 2000          # 允许透支额度（token）
    recover-stale-minutes: 10   # 预扣超时阈值（恢复用）
    reconcile-threshold: 100    # 对账修正阈值
    model-completion-cap:
      default: 2048             # 按模型的 completion 预留上界
  ratelimit:
    window-seconds: 60
    token-limit: 100            # 单 API Key 每窗口最大请求数
    model-limit: 50             # 单模型每窗口最大请求数
```

Redis key：`quota:{tokenId}`（余额）、`rl:token:{id}` / `rl:model:{name}`（滑动窗口）、`token:{hash}`（鉴权缓存）。

---

## API

OpenAI 兼容。错误返回 `{code, message, data, requestId}`，HTTP 状态码与错误对应（`402` 额度不足、`429` 限流、`502` 所有渠道失败、`503` 全部熔断 ……）。

---

## 部署分层

Janus 是**业务网关**。生产环境应在前面加一层**流量网关**（Nginx / 云 LB / WAF）：

```
Client → Nginx（TLS、负载均衡、IP/连接数限流、DDoS 防护）→ Janus × N → 上游
```

Janus 无状态（状态全在 MySQL/Redis），可在 LB 后水平扩容。IP 级限流归流量网关，Janus 聚焦 key/模型级（业务）限流。

---

## 路线图

| Phase | 状态 | 范围 |
|---|---|---|
| 0 | ✅ 已完成 | OpenAI 兼容透传 + SSE |
| 1 | ✅ 已完成 | 多渠道路由 + 故障转移 + 断路器 |
| 2 | ✅ 已完成 | API Key + 配额计费（Redis Lua 一致性、崩溃恢复） |
| 3 | ✅ 已完成 | 多级限流（滑动窗口） |
| 4 | 🔜 计划中 | Kafka 用量审计（把高频写从主链路剥离） |
| 5 | 🔜 计划中 | 多厂商 Adapter（Claude/Gemini）+ 管理后台 |

---

## 技术栈

Java 21 · Spring Boot 3.5（WebFlux）· MyBatis-Plus · MySQL 8 · Redis 7（Lettuce + Lua）· Resilience4j · jtokkit · Maven

---

## 目录结构

```
src/main/java/com/marsdev/janus/
├── controller/     RelayController（OpenAI 兼容入口）
├── filter/         AuthFilter、RateLimitFilter、QuotaPreCheckFilter、MeteringFilter
├── channel/        ChannelRouter（加权路由 + 断路器）
├── reply/          UpstreamProxy（SSE + 故障转移）、UsageParser
├── quota/          QuotaService（Lua）、Bootstrap、RecoverJob、ReconcileJob、估算器
├── ratelimit/      RateLimitService（滑动窗口）
├── entity / mapper / model / common
src/main/resources/
├── application.yml
└── lua/            quota_pre_consume.lua、quota_adjust.lua、ratelimit.lua
.sql/               建表 + 示例数据
scripts/            gen-token.sh（生成 API Key）
```

---

## License

[MIT](./LICENSE)
