# iped-runner — Evolution Roadmap

> Module purpose: Spring Boot service for launching and monitoring IPED processing —
> local executions and distributed (Kafka) cases — with a web dashboard (bundle built
> from `iped-runner-ui`). Packages: `iped.runner.{config,distributed,execution,web}`.
> Status legend: `[ ]` planned · `[~]` in progress · `[x]` done

## Current state (2026-06)
- [x] Dashboard shows distributed cases from the `iped.status` topic.
- [x] Launch script (`start-iped-runner.ps1`) and docker-compose integration with retry
      settings.
- Active branch: `feature/kafka-distributed-processing`.

## Phase 1 — Run lifecycle completeness
- [x] Full run state machine surfaced in the API: queued → running → completed/failed/
      timed-out, with the timeout/completion detection already built in the distributed
      module reflected accurately.
      (`RunStatus` enum: QUEUED/RUNNING/COMPLETED/FAILED/ABORTED/TIMED_OUT.
      `pumpOutput` sets COMPLETED or FAILED on process exit. `abort()` uses SIGTERM →
      configurable wait → SIGKILL. `GET /run/{id}` returns live status for active runs
      and history-backed status for terminal runs. `GET /runs` returns all history.)
- [x] Run history persistence (survive runner restarts; today's state is in-memory/topic
      derived) — small embedded DB or compacted topic, decision recorded here.
      (Chose JSON file (`run-history.json` in the runner work directory, configurable via
      `runner.history-file`). `RunHistoryService` persists after every terminal event and
      loads on startup. Compact and simple; upgrade path to SQLite/H2 is straightforward.)
- [x] Log streaming per run (tail engine/agent logs through the dashboard).
      (`GET /run/{id}/stream` streams SSE `log` events from the process stdout.
      `RunStats.tailLines(20)` feeds recent log lines into the dashboard snapshot.)
- [x] Cancel/abort a run cleanly (coordinate with distributed graceful-drain work).
      (`DELETE /run/{id}` now sends SIGTERM, waits `runner.graceful-shutdown-seconds`
      (default 5), then calls `destroyForcibly()` if the process is still alive.
      The aborted run is recorded in history with `RunStatus.ABORTED`.)

## Phase 2 — Scheduling and queueing
- [x] Case queue with priorities and concurrency limits.
      (`RunQueueService` wraps a `PriorityBlockingQueue<QueuedRun>` ordered by
      `RunPriority` (HIGH/NORMAL/LOW, each with a numeric weight) then enqueue time.
      A drainer thread blocks on the queue and a `Semaphore` with `runner.max-concurrent`
      permits (default 2), so at most N runs execute concurrently. `POST /run` now enqueues
      rather than launching directly. `GET /queue` returns pending (not-yet-started) runs.)
- [x] Profile/config selection per run from the dashboard.
      (`RunRequest` gains optional `priority` and `profile` fields. `QueueController`
      exposes `GET /profiles` listing `*.toml` files from `runner.profiles-dir`.
      The `--profile <name>` token is injected into the CLI args when provided.)
- [x] Batch submission: point at a folder of evidence, generate N runs.
      (`POST /runs/batch` accepts `{sourceDir, outputDir, priority?, profile?,
      extraTokens?[]}`; lists immediate children of `sourceDir`, enqueues one run per
      child with `-d <child> -o <outputDir>/<name>`, returns list of enqueued IDs.)

## Phase 3 — Operations
- [x] AuthN/authZ for the runner API.
      (`RunnerApiKeyFilter.java` — Spring `OncePerRequestFilter` at `@Order(1)`.
      Reads key from `runner.api-key` property. Accepts `X-Api-Key` or
      `Authorization: Bearer`. No-op when property is blank (open-access dev mode).
      Dashboard SSE (`/dashboard`), browse (`/browse`), and actuator endpoints are
      always passed through so the UI works without key management in the browser.)
- [x] Metrics endpoint.
      (`MetricsController.java` — `GET /metrics` returning flat JSON:
      `runner.active_runs`, `runner.queued_runs`, `runner.slots_total`,
      `runner.slots_available`, `runner.profiles_available`,
      `distributed.active_cases`, `distributed.kafka_available`,
      `jvm.heap_used_bytes`, `jvm.heap_max_bytes`, `jvm.uptime_ms`, `ts`.
      Kafka metrics absent when `DistributedStatusService` is not in context.)
- [x] Webhook notifications on run completion/failure.
      (`WebhookNotifier.java` — fire-and-forget HTTP POST on every terminal run
      event (COMPLETED, FAILED, ABORTED, TIMED_OUT). Configured via
      `runner.webhook.url` and optional `runner.webhook.secret` (sends as
      `Authorization: Bearer`). Payload: event name, run metadata, item counts.
      Delivery failures are logged at WARN and never affect run state. Wired into
      `ExecutionService.recordTerminal()` after history persistence.)
- [ ] Multi-node awareness: agent inventory view with health/heartbeats.
      (Deferred — requires Kafka heartbeat topic convention; tracked in Phase 4.)

## Phase 4 — 5.0 integration
- [x] Become the control-plane API of the root roadmap's target architecture (case/job
      orchestration), consumed by the browser UI and MCP `job_*` tools rather than
      having its own parallel UI surface long-term.
      (`RunJobsV2Controller.java` — `GET /v2/jobs`, `GET /v2/jobs/{id}`,
      `DELETE /v2/jobs/{id}`. Returns webapi-compatible JSON shape `{id, type:
      "iped-run", status, progress, message, createdAt, params}`. Status mapped to
      the same strings as `iped-webapi` JobsV2 (`pending/running/completed/failed/
      cancelled`). Progress derived as `min(99, round(100*processed/found))`. Active
      runs, queued runs, and history entries de-duplicated by id, so callers see one
      unified view.)
- [x] Contract alignment with `iped-webapi` job endpoints (one job model, not two).
      (Both modules now emit identical JSON job shapes, making the browser UI and MCP
      tools able to use either backend as a source of truth.)

## Phase 5 — Kafka security and chain-of-custody audit
- [x] Agent heartbeat inventory (`AgentInventoryService`, `AgentController`).
      (`AgentHeartbeatEvent` — mirrors `iped.agents` Kafka topic message.
      `AgentRecord` — computed health: active (<30 s), stale (30–120 s), offline (>120 s).
      `AgentInventoryService` — dedicated consumer group per runner instance, updates a
      `ConcurrentHashMap`; `isEnabled()` false when Kafka absent.
      `GET /v2/agents` returns `{enabled, total, active, stale, offline, agents[]}`;
      empty list (not 503) when Kafka absent.)
- [x] `agentId` field added to `StatusEvent` — stamped by the publishing agent so audit
      records carry full provenance.
- [x] `KafkaSecurityConfigurer` — static utility mapping a `KafkaSecurityConfig` record to
      Kafka client properties (TLS, SASL PLAIN/SCRAM-SHA-256/SCRAM-SHA-512). No-op when
      both TLS and SASL are disabled (PLAINTEXT mode).
- [x] `PayloadSigner` — HMAC-SHA256 over `"iped-v1|caseId|itemUuid|pipelineStage|path|lengthBytes"`,
      lowercase hex output. `sign()` / `verify()` static methods with constant-time
      comparison. `isEnabled(secret)` guard for rolling upgrade path.
- [x] `ProcessingRecord` — immutable chain-of-custody record created from terminal
      `StatusEvent` types (COMPLETED, ERROR, TIMEOUT); null for non-terminal events.
- [x] `ProcessingAuditLog` — `ConcurrentHashMap<caseId, CopyOnWriteArrayList<ProcessingRecord>>`.
      `record()`, `forCase()`, `forItem()`, `latestForItem()`, `exportCsv()`.
      Optional file persistence to append-mode CSV via `ProcessingAuditLog(Path)` constructor.
      Wired into `DistributedStatusService.handleMessage()`.
- [x] `AuditController` — `GET /api/v1/audit/{caseId}` (JSON, newest-first),
      `GET /api/v1/audit/{caseId}/csv` (RFC 4180 download),
      `GET /api/v1/audit/{caseId}/{itemUuid}` (latest terminal record). 404 when no records.

## Phase 5 — Multi-node agent inventory
- [x] Multi-node awareness: agent inventory view with health/heartbeats.
      (`AgentHeartbeatEvent.java` — Jackson record for `iped.agents` Kafka topic
      messages (`agentId`, `hostname`, `startedAt`, `lastSeen`, `version`,
      `activeTasks`, `maxTasks`). `AgentRecord.java` — immutable record with computed
      `health` field: `"active"` (<30 s), `"stale"` (30–120 s), `"offline"` (>120 s).
      `AgentInventoryService.java` — Kafka consumer with unique group ID (same
      `runner.kafka.bootstrap-servers` gate as `DistributedStatusService`); keeps the
      latest heartbeat per `agentId` via `ConcurrentHashMap.merge()`; `listAgents()`
      returns health recomputed at call time against `Instant.now()`. Disabled
      gracefully when Kafka is not configured.
      `AgentController.java` — `GET /v2/agents` returns `{enabled, total, active,
      stale, offline, agents[]}`. Returns empty list (not 503) when Kafka is absent
      so the dashboard panel always renders.)

## Progress checks
- A distributed run launched, monitored, and completed entirely from the dashboard.
- Runner restart mid-run → state recovered, run still tracked to completion.
