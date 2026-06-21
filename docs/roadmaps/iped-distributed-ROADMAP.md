# iped-distributed — Evolution Roadmap

> Module purpose: Kafka-based distributed processing — coordinator, processing agents,
> status reporting (`iped.distributed.{agent,config,coordinator,kafka,status}`).
> Implements workstream 4 of the root `ROADMAP.md`.
> Status legend: see linked issues in docs/issues/ for per-item status (canonical statuses: proposed, planned, in_progress, blocked, done, cancelled).

## Current state (2026-06)
- Active development branch (`feature/kafka-distributed-processing`).
- [x] Coordinator/agent split with Kafka topics for work distribution.
- [x] Hardening pass: retry with backoff, status consumer, timeout/completion detection.
- [x] Docker compose stack with runner dashboard and retry settings.
- [x] Minimal Testcontainers-backed Kafka integration tests for round-trip, status flow,
      topic provisioning, and poison-payload handling.
- Surfaced on the runner dashboard via the `iped.status` topic.

## Phase 1 — Correctness guarantees — `done`
- [ISSUE-057](../issues/ISSUE-057-at-least-once-delivery-idempotency.md) — At-least-once delivery with deterministic re-delivery idempotency — `done`
- [ISSUE-058](../issues/ISSUE-058-dead-letter-queue-operator-tooling.md) — Dead-letter queue with operator tooling — `done`
- [ISSUE-059](../issues/ISSUE-059-poison-message-handling.md) — Poison-message handling — `done`

## Phase 2 — Operability — `done`
- [ISSUE-060](../issues/ISSUE-060-distributed-metrics-export.md) — Consumer-lag, throughput, and per-stage error metrics export — `done`
- [ISSUE-061](../issues/ISSUE-061-status-event-versioned-schema.md) — Structured status events with a versioned schema — `done`
- [ISSUE-062](../issues/ISSUE-062-graceful-agent-drain.md) — Graceful agent drain for rolling restarts — `done`
- [ISSUE-063](../issues/ISSUE-063-coordinator-failover-story.md) — Coordinator failover story — `done`

## Phase 3 — Scale and scheduling — `done`
- [ISSUE-064](../issues/ISSUE-064-adaptive-work-unit-sizing.md) — Adaptive work-unit sizing strategy — `done`
- [ISSUE-065](../issues/ISSUE-065-multi-case-priority-scheduling.md) — Multi-case scheduling across the agent pool with priorities — `done`
- [ISSUE-066](../issues/ISSUE-066-resource-pressure-backpressure.md) — Backpressure to the coordinator on agent resource pressure — `done`

## Phase 4 — Production rollout (5.0) — `done`
- [ISSUE-067](../issues/ISSUE-067-dual-run-strangler-mode.md) — Dual-run mode (strangler pattern) for cutover validation — `done`
- [ISSUE-068](../issues/ISSUE-068-security-tls-signing-audit-trail.md) — Security — TLS/auth on Kafka, signed payloads, chain-of-custody audit trail — `done`
- [ISSUE-069](../issues/ISSUE-069-deployment-playbook.md) — Deployment playbook — `done`

## Phase 5 — Production hardening — `done`
- [ISSUE-070](../issues/ISSUE-070-agent-steering-subscription-cap.md) — Multi-case agent steering with dynamic topic subscription and cap — `done`
- [ISSUE-071](../issues/ISSUE-071-config-validation-startup.md) — Config validation at coordinator startup — `done`
- [ISSUE-072](../issues/ISSUE-072-case-lifecycle-rest-additions.md) — Case lifecycle REST additions — deletion, progress, work-unit tagging, error tracking — `done`
- [ISSUE-073](../issues/ISSUE-073-registration-retry-rate-limited-dlq-requeue.md) — Agent registration retry and rate-limited DLQ requeue — `done`

35 broker-free tests across `Phase5HardeningTest` covering all of the above. Full suite: 440 tests, 0 failures.

## Phase 6 — Observability and robustness — `done`
- [ISSUE-074](../issues/ISSUE-074-observability-robustness-batch.md) — Phase 6 observability batch — work-unit wiring, case ops, event log, DLQ stats — `done`

35 broker-free tests in `Phase6ObservabilityTest`. Full suite: 475 tests, 0 failures.

## Phase 7 — Resilience validation — `done`
- [ISSUE-075](../issues/ISSUE-075-chaos-and-lifecycle-integration-tests.md) — Multi-agent chaos simulation and coordinator lifecycle integration tests — `done`

Full suite: 495 tests, 0 failures.

## Phase 8 — Pipeline depth and sub-item propagation — `done`
- [ISSUE-076](../issues/ISSUE-076-pipeline-depth-subitem-propagation-tests.md) — Multi-stage pipeline depth and sub-item propagation correctness — `done`

Full suite: 514 tests, 0 failures.

## Phase 9 — Testcontainers integration test expansion — `done`
- [ISSUE-077](../issues/ISSUE-077-testcontainers-integration-test-expansion.md) — Testcontainers integration test expansion — `done`

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
