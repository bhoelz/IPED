# iped-distributed — Evolution Roadmap

> Module purpose: Kafka-based distributed processing — coordinator, processing agents,
> status reporting (`iped.distributed.{agent,config,coordinator,kafka,status}`).
> Implements workstream 4 of the root `ROADMAP.md`.
> Status legend: `[ ]` planned · `[~]` in progress · `[x]` done

## Current state (2026-06)
- Active development branch (`feature/kafka-distributed-processing`).
- [x] Coordinator/agent split with Kafka topics for work distribution.
- [x] Hardening pass: retry with backoff, status consumer, timeout/completion detection.
- [x] Docker compose stack with runner dashboard and retry settings.
- [x] Minimal Testcontainers-backed Kafka integration tests for round-trip, status flow,
      topic provisioning, and poison-payload handling.
- Surfaced on the runner dashboard via the `iped.status` topic.

## Phase 1 — Correctness guarantees
- [x] At-least-once delivery: `TaskAgent` switched from auto-commit to manual offset
      commit via `PartitionOffsetTracker`; offsets advance only after the forwarded record
      is durably acknowledged by the broker (or parked in DLQ). Interrupted items are
      intentionally left uncommitted for re-delivery.
- [x] Re-delivery idempotency: sub-item UUIDs are now derived deterministically from
      `(parentItemUuid, ordinal)` via `ItemConverter.deterministicSubitemUuid()` (UUID v3,
      MD5 name-based, stable across restarts). `buildSubitemRegistry` assigns ordinals in
      emission order; task code must emit sub-items in a stable order for the same input
      (all built-in tasks satisfy this). Parent items are naturally idempotent via Lucene
      `updateDocument`. The full contract is documented in `TaskAgent`'s class javadoc.
- [x] Dead-letter queue with operator tooling: `DlqManager` (list/requeue/discard) wired
      into `CoordinatorServer` as three REST endpoints:
      `GET /api/v1/dlq/{caseId}?limit=N`,
      `POST /api/v1/dlq/{caseId}/requeue` (body: `{"positions":[…]}`),
      `POST /api/v1/dlq/{caseId}/discard` (body: `{"positions":[…]}`).
      Requeue resets `attempt=0` and `notBeforeMs=0`; both operations commit the DLQ
      offset in the `iped.dlq.ops.{caseId}` consumer group so the item is not returned
      by future list calls.  List is a stateless peek (random group, no commit).
- [x] Exactly-once-effect validation: property-based simulation confirms that at-least-once
      delivery + deterministic sub-item UUIDs + upsert indexing = exactly-once observable
      effect. `ExactlyOnceEffectTest` simulates agent crashes at every commit point (0, 1,
      3, 5, 10) against a monolithic baseline; verifies: no items lost, no items duplicated,
      sub-item UUIDs stable across redeliveries, `PartitionOffsetTracker` watermark stops
      at the first uncommitted gap (triggering correct re-delivery range), poison items
      DLQ'd without corrupting the output index.
- [x] Poison-message handling: a malformed evidence segment must not stall the partition.
      `KafkaItemDeserializer` catches all parse exceptions and returns a sentinel instead of
      throwing; `TaskAgent.processRecord` detects the sentinel via `KafkaItemDeserializer.isPoison()`
      and routes directly to the stage DLQ (attempt=0, no task execution), then commits the offset.
      The sentinel carries `__poison.error` (exception message) and `__poison.rawSnippet`
      (hex prefix of raw bytes) in `extraAttributes` for operator inspection via `DlqManager.list`.

## Phase 2 — Operability
- [x] Consumer-lag, throughput, and per-stage error metrics exported (Prometheus format).
      `DistributedMetrics` maintains thread-safe `LongAdder` counters keyed by
      (case, task-type, stage): `items_processed_total`, `items_failed_total`,
      `processing_duration_ms_total`.  The coordinator's status-event callback now fans
      out to both `CaseCompletionMonitor` and `DistributedMetrics.recordEvent()`.
      `ConsumerLagProvider` queries `AdminClient` for log-end vs committed offsets at
      scrape time; returns an empty map (not an error) when Kafka is unavailable so the
      endpoint stays up.  `GET /metrics` on the coordinator serves Prometheus text-format
      (content-type `text/plain; version=0.0.4`) with counters, per-agent in-flight/free
      gauges from `AgentRegistry`, and optional consumer-lag gauges for all active cases.
- [x] Structured status events with a versioned schema (`specs/88-distributed-status-events-schema.md`);
      schema evolution policy for the `iped.status` topic.  `ItemStatusEvent` carries a
      `schemaVersion: int` field (current value `1`; legacy events deserialize to `0`).
      All factory methods stamp the version.  `ItemStatusConsumer.validateSchemaVersion()`
      logs a warning on future versions (forward-compat via `@JsonIgnoreProperties`) and
      accepts v0 as a pre-versioned equivalent.  Full field catalogue, JSON examples,
      topic-config table, and evolution rules documented in `specs/88-distributed-status-events-schema.md`.
- [x] Graceful agent drain (finish current segment, commit, exit) for rolling restarts.
      `TaskAgent.drain()` sets a draining flag; the poll loop pauses all Kafka partitions
      (no new records), waits for `inFlight == 0`, calls `producer.flush()` so every async
      send callback (`markDone`) fires, then exits.  Also fixed a shutdown ordering bug:
      `producer.flush()` is now called before `drainCommittable()`+`commitSync()` in ALL
      exit paths (both drain and hard `stop()`), ensuring no completed item misses its
      final offset commit.  `stop()` retains hard-exit semantics; `drain()` is for SIGTERM.
- [x] Coordinator failover story: documented and tested in `specs/89-coordinator-failover.md`.
      Key insight — the coordinator is NOT in the data path, so agents keep processing
      across a coordinator outage; only observability/auto-detection pauses.  Two recovery
      mechanisms added: (1) `CaseLifecycleManager` optionally persists case definitions
      (the only state not reconstructable from Kafka, since status events lack the ordered
      task list) to `cases.json` via the new `coordinatorStateDir` config, reloaded on
      startup; (2) `CaseCompletionMonitor.recover()` replays the `iped.status` topic
      (bounded, throwaway group, no commits — `StatusTopicReplayer`) to rebuild completion
      progress with side effects suppressed (no re-published CASE_COMPLETED / TIMEOUT).
      `AgentRegistry` self-heals via heartbeats.  Multi-coordinator HA is explicitly NOT
      yet supported (no leader election) — run one supervised coordinator; HA path to
      leader election is documented as Phase 4 follow-up.  13 broker-free tests in
      `CoordinatorFailoverTest` cover persistence round-trip and progress recovery.

## Phase 3 — Scale and scheduling
- [x] Work-unit sizing strategy: adaptive segment sizes based on evidence type/size
      instead of fixed splits.  `iped.distributed.workunit.AdaptiveWorkUnitPlanner` packs
      items greedily by *weighted* size (raw bytes × `MediaCostModel` per-type cost: videos
      4×, disk images 3.5×, archives 3×, images 1.5×, dirs/empty 0.1×) under a
      `WorkUnitSizingPolicy` (soft `targetUnitBytes`=64 MB, hard `maxUnitItems`=256,
      `oversizedItemBytes`=128 MB isolation).  Tiny files batch together (amortising
      per-message overhead); a single huge/expensive item is isolated into its own
      `oversized` unit (no load skew).  The plan is a pure, deterministic function of
      (input order, policy, cost model) — preserving the Phase-1 exactly-once-effect
      guarantee.  Config knobs `workUnit*` in `DistributedConfig`; documented in
      `specs/90-adaptive-work-unit-sizing.md`.  Producer/agent wiring to dispatch a
      `WorkUnit` as a compound message is the documented follow-up; the sizing logic,
      cost model, and 22 broker-free tests (`AdaptiveWorkUnitPlannerTest`) land here.
- [x] Multi-case scheduling across the agent pool with priorities (urgent cases preempt
      queue order, not running segments).  `iped.distributed.scheduler.CaseScheduler`
      distributes free agent slots across active cases by `CasePriority`
      (URGENT>HIGH>NORMAL>LOW): strict priority across classes (urgent fully served before
      normal — preempts queue order), round-robin within a class (equal cases share fairly),
      capped at each case's pending work, and **free-slots-only** (never preempts in-flight
      items).  Deterministic given (priorities, pending work, free slots).  Priority is part
      of the persisted case definition (`CaseStatus.priority`, default NORMAL, survives
      coordinator restart) with REST endpoints: `priority` field on `POST /cases/start` and
      `POST /api/v1/cases/{caseId}/priority` to re-prioritise.  The coordinator feeds the
      scheduler per task type from `CaseLifecycleManager` priorities + `ConsumerLagProvider`
      pending work + `AgentRegistry` free slots.  Documented in
      `specs/91-multi-case-scheduling.md`; 14 scheduler tests (`CaseSchedulerTest`) + 3
      priority-persistence tests in `CoordinatorFailoverTest`.  Agent-side enforcement
      (steering a typed agent across case topics per the plan) is the documented wiring
      follow-up, mirroring the work-unit sizing/dispatch split.
- [x] Backpressure to the coordinator when agent storage/memory nears limits, integrated
      with the engine's ResourceManager.  `iped.distributed.resource` package:
      `PressureLevel` (NONE/SOFT/HARD), `ResourcePressure` (snapshot record),
      `ResourcePressurePolicy` (thresholds: heap 75%/90%, disk 10 GB/5 GB soft/hard),
      `ResourcePressureMonitor` (injected samplers: heap via `Runtime`, disk via
      `Files.getFileStore`, engine via `ResourceManager.enforceQuotas`; `forAgent()`
      factory wires live samplers; fully testable via synthetic suppliers).
      `TaskAgent.heartbeatLoop()` samples on every heartbeat tick, logs level transitions
      (WARN on HARD, INFO on recovery), and calls the 5-arg `coordinator.heartbeat()`.
      The main poll loop pauses all Kafka partitions while `lastPressure==HARD` (in-flight
      items always finish); resumes automatically when pressure drops — the same partition-
      pause mechanism as graceful drain but without draining in-flight work.
      Coordinator side: `AgentRegistry.schedulableFreeSlots(taskType)` excludes HARD
      agents (they retain their registered `freeSlots`; only the *schedulable* view hides
      them); `pressuredAgentCount(taskType)` for observability; HARD transition logged at
      WARN.  Heartbeat carries `pressureLevel`/`pressureRatio`; `CoordinatorServer`
      parses and forwards both fields.  All thresholds configurable via `DistributedConfig`
      (`backpressure*` keys).  25 broker-free tests in `ResourcePressureTest` cover policy
      validation, heap/disk/engine thresholds, combined-signal worst-wins, level helpers,
      registry exclusion, and `ResourcePressure.accepting()`.  Documented in
      `specs/92-backpressure.md`.

## Phase 4 — Production rollout (5.0)
- [x] Dual-run mode (strangler pattern): same case processed both paths, results diffed
      automatically — exit criterion for cutover.  New `iped.distributed.dualrun` package:
      `ItemSummary` (uuid, path, mediaType, lengthBytes; extracted from DISCOVERED events
      or submitted by the operator for the reference side), `DualRunVerdict`
      (INCOMPLETE/MATCH/MISMATCH), `AttributeMismatch`, `DualRunReport` (verdict,
      per-side counts, capped discrepancy lists with true-count fields, `summary()`),
      `DualRunComparator` (stateless; path-based matching; compares coverage + non-null
      attributes; list capped at 500), `DualRunSession` (per-case thread-safe accumulator;
      feeds on DISCOVERED/SUBITEM_DISCOVERED events, marks done on CASE_COMPLETED),
      `DualRunManager` (coordinator-side map; no-op event routing when no session exists).
      `ItemStatusEvent` enriched with `mediaType`/`lengthBytes` (non-breaking: new fields,
      backward-compatible deserialization) so distributed items carry rich metadata for
      comparison.  `DistributedConfig.dualRunEnabled` (default false): when true, a session
      is auto-opened for every new case.  Three new REST endpoints on the coordinator:
      `POST /api/v1/dualrun/{caseId}/start` (manual session open),
      `POST /api/v1/dualrun/{caseId}/reference` (submit monolithic item list as JSON),
      `GET  /api/v1/dualrun/{caseId}` (get current comparison report).  25 tests in
      `DualRunComparatorTest` cover INCOMPLETE/MATCH/MISMATCH paths, attribute comparison,
      session lifecycle, and manager routing.
- [x] Security: TLS + auth on Kafka, signed work-unit payloads, audit trail of which
      agent processed which evidence (chain-of-custody requirement).
      `iped.distributed.security`: `KafkaSecurityConfigurer` applies TLS (`ssl.*`) and/or
      SASL (PLAIN / SCRAM-SHA-256 / SCRAM-SHA-512) properties to any Kafka `Properties`
      map; wired into `TaskAgent.buildConsumer()`/`buildProducer()`.  New config keys:
      `kafkaTlsEnabled`, `kafkaTruststorePath/Password`, `kafkaKeystorePath/Password/Key`,
      `kafkaSaslMechanism/Username/Password`.  `PayloadSigner` (HMAC-SHA256 over immutable
      message fields `caseId|itemUuid|stage|path|length`): `sign()` before forwarding,
      `verify()` on receive — failure routes to DLQ; enabled via `payloadSigningSecret` config
      key; `KafkaItemMessage.signature` field added (`@JsonIgnoreProperties` compat).
      `iped.distributed.audit`: `ProcessingRecord` (immutable per-event chain-of-custody
      snapshot: itemUuid, caseId, taskType, stage, agentId, processedAt, durationMs,
      outcome, errorMessage); `ProcessingAuditLog` (thread-safe accumulator; ignores
      non-terminal events; `forCase`/`forItem`/`latestForItem` queries; `exportCsv()`
      for forensic reporting with RFC-4180 escaping; optional durable file append via
      `auditLogPath` config).  `ItemStatusEvent` enriched with `agentId` field (non-breaking);
      `ItemStatusProducer(bootstrapServers, agentId)` 2-arg constructor; `TaskAgent` passes
      its own agentId so every status event carries it.  Three new coordinator REST endpoints:
      `GET /api/v1/audit/{caseId}` (JSON), `GET /api/v1/audit/{caseId}/csv` (CSV download),
      `GET /api/v1/audit/{caseId}/{itemUuid}` (per-item records).  33 broker-free tests in
      `SecurityTest` cover TLS/SASL config properties, SASL JAAS config generation, payload
      sign/verify/tamper/wrong-secret scenarios, `ProcessingRecord` factory, audit log
      accumulation/isolation/querying, CSV export, and file persistence append.
- [x] Deployment playbook (topic creation, partitioning, retention, security, monitoring,
      dual-run validation, cutover) in `DEPLOYMENT-GUIDE.md`.

## Phase 5 — Production hardening

- [x] Agent-side multi-case dynamic subscription: the coordinator heartbeat response now
      carries `subscribedTopics` — the Kafka input-stage topics an agent should watch,
      computed from all RUNNING cases that include its task type.  Agents apply the update
      from the consumer thread on the next poll iteration (volatile `pendingTopicUpdate`
      handoff pattern, same as backpressure).  New `TaskAgent(taskType, stageNumber, ...)` 
      constructor (5-arg, no fixed caseId) creates a multi-case agent with consumer group
      = `taskType` (shared offset tracking across all case topics); the existing 6-arg
      single-case constructor is preserved for backward compatibility.  Dynamic topic
      derivation in `processRecord()`, `handleFailure()`, `sendToDeadLetterQueue()`, and
      `buildSubitemRegistry()` now uses the message's own `caseId` instead of a fixed field
      — correct for both modes.  New `HeartbeatResponse` DTO; `CoordinatorClient.heartbeat()`
      returns it (was `void`); new `AgentTopicAssigner` (testable coordinator-side helper;
      excludes COMPLETED/FAILED cases; sorts topics for deterministic comparison).
      `AgentRegistry.getAgent(agentId)` accessor added.  21 broker-free tests in
      `AgentSteeringTest` cover HeartbeatResponse construction/serialization, AgentTopicAssigner
      single/multi-case/completed/excluded scenarios, determinism, and JSON parsing.
- [x] Config validation: `ConfigValidator.validate(DistributedConfig)` returns a list of
      actionable error messages; callers fail-fast at startup.  Rules cover blank required
      strings (kafkaBootstrapServers, coordinatorServerUrl, sharedStorageRoot), port range,
      positive timeouts, heartbeat < expiry invariant, partitions/replication/parallelism ≥ 1,
      work-unit oversized ≥ target, backpressure heap soft < hard, disk hard < soft,
      SASL username required when mechanism is set, keystore password required when path is set.
      Wired into `CoordinatorServer.main()`.  15 broker-free tests in `Phase5HardeningTest`.
- [x] Work-unit tagging: `KafkaItemMessage` gains `workUnitId` (string) and `workUnitIndex`
      (int) fields — assigned by `AdaptiveWorkUnitPlanner` at ingestion time so operators can
      correlate items in logs and metrics.  Both fields are `@JsonIgnoreProperties`-safe (null
      in sub-items discovered mid-pipeline).  4 broker-free tests.
- [x] Case deletion endpoint: `DELETE /api/v1/cases/{caseId}` deprovisions Kafka topics via
      `TopicProvisioner.deprovisionCase()` and removes the case from all lifecycle maps.
      `CaseLifecycleManager.deleteCase()` returns `false` for unknown cases; the endpoint
      maps that to HTTP 404.  Persistence: the durable `cases.json` is updated atomically.
      3 broker-free tests covering success, 404, and allCases cleanup.
- [x] Coordinator REST additions (5 new endpoints):
        `GET  /api/v1/cases/{caseId}/progress`  — `{discovered, completedFinal, inFlight,
            failed, completionPct}` from `CaseCompletionMonitor`; 404 for unknown cases.
        `GET  /api/v1/health`                   — `{status:"ok", activeCases, liveAgents,
            uptimeMs}` for load-balancer liveness probes and monitoring.
        `GET  /api/v1/dlq/{caseId}/count`       — lightweight entry count (begin-to-end offset
            difference per DLQ partition) for monitoring dashboards without pulling full entries.
        `POST /api/v1/dlq/{caseId}/requeue`     — now accepts optional `delayMs` body field;
            sets `notBeforeMs = now + delayMs` so agents honour a cool-down before retry.
        `DELETE /api/v1/cases/{caseId}`         — see case deletion above.
- [x] Error tracking in CaseCompletionMonitor: `CaseProgress` now maintains a `failedCount`
      `AtomicLong` incremented on every ERROR status event; exposed via `failedCount(caseId)`.
      Surfaced in `GET /api/v1/cases/{caseId}/progress` as the `failed` field.  Isolated per
      case; 0 for unknown cases.  4 broker-free tests.
- [x] Agent registration retry: `CoordinatorClient.registerWithRetry(reg, maxAttempts,
      baseDelayMs)` retries with exponential backoff (delay = base × 2^attempt, capped at
      bit-shift 10 to avoid overflow); throws `RuntimeException` after exhausting all attempts.
      Existing `register()` remains a fire-and-forget best-effort call for backward compat.
      3 broker-free tests: success on first attempt, success after retries, exhaustion throws.
- [x] Scheduler-driven subscription cap: `DistributedConfig.maxSubscribedCases` (default 0 =
      unlimited) caps how many case topics the heartbeat response returns to each multi-case
      agent.  `AgentTopicAssigner(registry, lifecycle, maxCap)` applies the cap by selecting
      the highest-priority running cases (sorted by `CasePriority.weight()` descending, then
      case ID ascending for determinism within the same class) before mapping to topics; result
      is always sorted lexicographically so `List.equals` comparison on the agent side is
      reliable.  `CoordinatorServer.start()` passes `cfg.getMaxSubscribedCases()` to the
      assigner.  6 broker-free tests in `Phase5HardeningTest`: zero cap (all), cap-1/cap-2
      (priority ordering), cap > count (all returned), same-priority alpha tie-break, sorted
      output.
- [x] Rate-limited DLQ requeue: `DlqManager.requeue(caseId, positions, delayMs)` sets
      `notBeforeMs = System.currentTimeMillis() + delayMs` when `delayMs > 0`, otherwise
      `notBeforeMs = 0` (immediate, same as before).  The zero-arg overload delegates to the
      new one for backward compatibility.  `POST /api/v1/dlq/{caseId}/requeue` parses the
      optional `delayMs` body field and echoes it in the response.  `DlqManager.count(caseId)`
      returns the total unconsumed entry count across all DLQ partitions (begin-to-end offset
      difference) without committing; used by the new count endpoint.
  35 broker-free tests across `Phase5HardeningTest` covering all of the above.
  Full suite: 440 tests, 0 failures.

## Phase 6 — Observability and robustness

- [x] Work-unit producer wiring: `KafkaItemProducer` buffers published items and calls
      `AdaptiveWorkUnitPlanner.plan(buffer)` on each add.  Completed work units are flushed
      immediately (all-but-last pattern); a final `flush()` sends any remaining partial unit.
      Items within a unit are tagged with `workUnitId = "{caseId}.wu.{N:06d}"` and
      `workUnitIndex` = their position within the unit.  Work-unit counter is monotone per
      producer instance; the 4-arg constructor (policy injected) enables unit testing.
      5 broker-free tests in `Phase6ObservabilityTest` verify planner grouping, max-item
      splitting, and input-order preservation.
- [x] Case pause/resume: `CaseLifecycleManager.pauseCase(caseId)` transitions RUNNING→PAUSED
      and persists the state.  `resumeCase(caseId)` transitions PAUSED→RUNNING.  Both are
      idempotent in the happy path and return `false` for unknown cases or invalid transitions
      (pause of a completed case; resume of a non-paused case).  Exposed as
      `POST /api/v1/cases/{caseId}/pause` and `POST /api/v1/cases/{caseId}/resume`;
      responses carry a `"paused"/"running"` status field.  `AgentTopicAssigner` already
      filters to RUNNING-only, so agents stop receiving a paused case's topics on the next
      heartbeat cycle without any extra wiring.  5 broker-free pause tests + 3 assigner tests.
- [x] Case tags / metadata: `CaseLifecycleManager.updateTags(caseId, tags)` merges a
      `Map<String,String>` into `CaseStatus.metadata` (existing keys overwritten; null values
      permitted for removal semantics in callers).  Persisted to `cases.json` atomically.
      `PATCH /api/v1/cases/{caseId}/tags` endpoint accepts a JSON object and mirrors the
      merged metadata in the response.  4 broker-free tests: success, merge, overwrite, 404.
- [x] Case stall detection: `CaseCompletionMonitor.CaseProgress` tracks `lastEventAtMs`
      (AtomicLong, updated on every status event).  `isStalled(caseId, stallWindowMs)` returns
      true when: discovered > 0, completedFinal < discovered, inFlight is empty, AND the silence
      since the last event ≥ stallWindowMs.  Returns false for unknown, zero-discovered, all-
      completed, or in-flight cases.  Accessed via `lastEventAtMs(caseId)`.  6 broker-free tests.
- [x] Coordinator event log: `CoordinatorEventLog` is a thread-safe ring buffer (default
      capacity 500) of `CoordinatorEvent` records (`type`, `caseId`, `agentId`, `message`,
      `timestamp`).  `EventType` covers 13 coordinator lifecycle transitions.  Static factory
      methods `caseEvent()`, `agentEvent()`, `generic()`.  `latest(n)` returns the n most
      recent events, oldest-first.  When full, oldest entries are silently overwritten.
      Wired into `CoordinatorServer` and emitted on case start/pause/resume/delete/complete.
      Exposed as `GET /api/v1/events?limit=N`.  8 broker-free tests covering empty, single,
      latest-n cap, ordering, ring-buffer wrap-around, size capping, and agent-event fields.
- [x] Agent pool summary / detail endpoints:
      `GET /api/v1/agents/summary` — groups agents by `taskType` and `state` (IDLE/BUSY/OVERLOADED),
      returns a map of `{taskType → {total, idle, busy, overloaded}}`.
      `GET /api/v1/agents/{agentId}` — full agent record (registration, last heartbeat,
      capacity, current state); 404 for unknown agents.
- [x] DLQ per-topic stats: `DlqManager.topicStats(caseId)` returns a `List<DlqTopicStats>`,
      one record per DLQ topic (format: `iped.{caseId}.stage.N.dlq`), with fields
      `dlqTopic`, `originalTopic`, `count`, `oldestOffset`, `newestOffset`.  Uses
      `AdminClient.listOffsets` for lightweight count; no consumption required.
      Exposed as `GET /api/v1/dlq/{caseId}/stats`.
- [x] DLQ auto-retrier: `DlqAutoRetrier` is a coordinator-side background service.  Each
      sweep iterates RUNNING cases, lists DLQ entries, and requeues those with
      `attempt < maxAttempts` using the configured `dlqAutoRetryDelayMs`.  Controlled by
      three new `DistributedConfig` keys: `dlqAutoRetryMaxAttempts` (0 = disabled, default),
      `dlqAutoRetryDelayMs` (60 000 ms default), `dlqAutoRetryIntervalSeconds` (120 default).
      Scheduled by `CoordinatorServer` on startup when enabled.  4 broker-free config tests;
      defaults verified via `Phase6ObservabilityTest`.
- [x] Batch case status: `GET /api/v1/cases/batch-status?ids=c1,c2,…` returns a JSON map
      of `{caseId → {state, discovered, completedFinal, failed, priority}}` in a single
      round-trip for dashboard polling efficiency.
  35 broker-free tests in `Phase6ObservabilityTest`.
  Full suite: 475 tests, 0 failures.

## Phase 7 — Resilience validation

- [x] Multi-agent chaos simulation: `MultiAgentChaosTest` simulates N agents each owning a
      partition, with K agents killed mid-run (uncommitted records reset to last committed
      watermark via `PartitionOffsetTracker`).  Three invariants are verified across all crash
      patterns: (1) no item loss — every input UUID and its deterministic sub-item UUID appear
      in the final upsert index; (2) no duplicates — upsert semantics ensure each UUID appears
      exactly once even across redeliveries; (3) case completion — `CaseCompletionMonitor`
      detects completion despite crash-and-restart cycles.  Covers: no-crash baseline, single
      crash, all-agents crash, repeated crash on same agent, deterministic sub-item UUID
      stability, upsert idempotency, and `PartitionOffsetTracker` watermark gap-stop + fill.
      10 broker-free tests in `MultiAgentChaosTest`.
- [x] Coordinator lifecycle integration: `CoordinatorLifecycleIntegrationTest` drives the five
      coordinator components through coherent multi-step scenarios that cannot be caught by
      isolated unit tests.  Scenarios: (1) single case/agent happy-path → completion; (2)
      pause/resume changing topic assignments in lock-step with the event log; (3) two cases
      with specialised agents — no topic cross-contamination; (4) case deletion removing topics
      from assigner and lifecycle simultaneously; (5) priority cap returning only the top-N
      highest-priority cases; (6) CASE_COMPLETED event logged to `CoordinatorEventLog` via the
      monitor's publisher callback; (7) stall detection → `CASE_STALLED` event log emit; (8)
      agent expiry driven by `liveAgents()` eviction; (9) two cases completing independently
      without interference; (10) full coordinator-restart recovery via `cases.json` + monitor
      replay, with live item completing post-recovery triggering exactly one CASE_COMPLETED.
      10 broker-free tests in `CoordinatorLifecycleIntegrationTest`.
  Full suite: 495 tests, 0 failures.

## Phase 8 — Pipeline depth and sub-item propagation

- [x] Multi-stage pipeline correctness: `MultiStagePipelineTest` verifies that a 3-task
      pipeline (HashTask→SignatureTask→IndexTask) routes items through stages correctly.
      Key invariant: `CaseCompletionMonitor.isFinalStage` only fires `completedFinal` at the
      last stage (index 2); intermediate completions at stages 0 and 1 do NOT count toward
      completion.  In-flight tracking is per-item-per-task (flight key = uuid|taskType), so an
      item can be simultaneously tracked across a stage transition.  Item timeouts at an
      intermediate stage emit TIMEOUT without incrementing completedFinal.  Covers: final-only
      counting, intermediate-stage ignored, multi-item multi-stage tracking, timeout at
      non-final stage, and case completion only after all items clear the last stage.
      10 broker-free tests in `MultiStagePipelineTest`.
- [x] Sub-item discovery mid-pipeline: `SubitemDiscoveryTest` verifies that
      `SUBITEM_DISCOVERED` events correctly grow `discovered` after items are initially
      registered, preventing premature case completion.  Key scenarios: sub-items discovered
      during processing hold the case open until they too complete the final stage; multiple
      sub-items from the same parent each require independent completion; mixed original-item
      and sub-item completions satisfy the discovered≤completed check exactly; and a case with
      only sub-items (all originals produce children) completes only when all children clear
      the final stage.  9 broker-free tests in `SubitemDiscoveryTest`.
  Full suite: 514 tests, 0 failures.

## Phase 9 — Testcontainers integration test expansion

- [x] DLQ lifecycle IT: `DlqManagerIT` exercises the full operator workflow against a real
      Kafka broker (Testcontainers `apache/kafka:3.8.1`).  Tests: `list()` returns correct
      entries with preserved `attempt` and `stage` fields; `list()` is idempotent (non-
      destructive peek, no offset committed); `count()` reflects exact message count;
      `count()` returns 0 when no DLQ topics exist; `topicStats()` returns the correct
      offset range (oldest=0, newest=N-1, count=N); `requeue()` moves items to the original
      stage topic with `attempt` reset to 0 and the original UUID intact; `requeue()` with
      delay stamps `notBeforeMs ≥ now+delayMs` on the republished message; `requeue()` for
      multiple items returns the requeued count and delivers all items to the stage topic.
      8 broker-backed tests in `DlqManagerIT`.
- [x] Status topic IT: `StatusTopicIT` verifies the `iped.status` producer/consumer pipeline
      against a real broker.  Tests: two independent consumer groups (different `groupId`)
      both receive all published events — Kafka fan-out; the 2-arg `ItemStatusProducer`
      constructor stamps `agentId` on every event while the 1-arg coordinator-mode constructor
      leaves `agentId` null; all 7 major event types (DISCOVERED, SUBITEM_DISCOVERED, STARTED,
      COMPLETED, ERROR, SKIPPED, CASE_COMPLETED) survive JSON serialization through Kafka and
      are delivered to the consumer; a fresh `ItemStatusConsumer` with `fromBeginning=true`
      reads a previously-published case lifecycle from offset 0 and a new
      `CaseCompletionMonitor` correctly rebuilds `discovered=1`, `completedFinal=1`, and
      `isCompleted=true`; events for the same `caseId` key all land in the same partition
      (consistent hash key contract).  5 broker-backed tests in `StatusTopicIT`.
- [x] DLQ discard IT: `DlqManagerIT` extended with `discard_itemIsNotForwardedToStageTopic` —
      publishes 2 items to DLQ, discards one, requeues the other, and asserts only the requeued
      item appears on the original stage topic.  Distinguishes `discard` (commit + no-forward)
      from `requeue` (commit + forward with attempt reset) in a live-broker scenario.
- [x] Multi-stage DLQ discovery IT: `DlqManagerIT` extended with
      `multiStage_listAndCount_aggregateAcrossBothDlqTopics` — creates a 2-stage case with a DLQ
      topic per stage, asserts `list()` aggregates entries across both topics, each entry's
      `pipelineStage` field reflects where it failed, `count()` sums both topics, and
      `topicStats()` returns one entry per DLQ topic.  Exercises the
      `dlqTopicsForCase(caseId)` admin-topic-discovery path with multiple matching topics.
- [x] At-least-once delivery IT: `AtLeastOnceDeliveryIT` exercises `PartitionOffsetTracker`
      with a real Kafka broker.  Three tests: (1) commit-watermark-only: consumer 1 polls 5
      records, marks 0-2 done, drains committable (offset 3 committed), closes; consumer 2 in
      same group receives exactly records 3 and 4 — none of 0-2 redelivered; (2) gap-stop: marks
      0, 2, 3 done but not 1, drains committable (watermark stops at 0 because of gap at 1),
      consumer 2 receives records 1, 2, 3; (3) all-committed: marks all done, consumer 2 gets
      nothing.  Together these prove the watermark semantics against a real broker, not just
      in-memory simulation.
- [x] Fixed flaky `@TempDir` cleanup on Windows in `CoordinatorLifecycleIntegrationTest`:
      `CaseLifecycleManager.persistState()` now uses `mapper.writeValueAsBytes()` +
      `Files.write()` (NIO, closes handle before rename) instead of `mapper.writeValue(File)`;
      `loadState()` uses `mapper.readValue(Files.readAllBytes())` for the same reason.
  All IT tests skip gracefully when Docker is unavailable (`disabledWithoutDocker = true`).
  IT suite: 22 tests (4 original + 10 DLQ + 5 status + 3 delivery), all skipped without Docker.
  Full unit suite: 514 tests, 0 failures, 0 errors (stable across repeated runs).

## Constraints
- Kafka types stay inside this module; the engine sees only lifecycle interfaces.
- Every message schema change needs forward-compatibility (old agents during rolling
  upgrade) or an explicit version gate.

## Progress checks
- Chaos test: N agents, kill K mid-run → case completes, counts match monolithic run.
- DLQ drill: inject poison message → partition keeps flowing, message lands in DLQ.
