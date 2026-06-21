# ISSUE-077: Testcontainers integration test expansion

- Status: done
- Roadmap: [iped-distributed-ROADMAP.md](../roadmaps/iped-distributed-ROADMAP.md)
- Roadmap section: Phase 9 — Testcontainers integration test expansion
- Owner: unassigned
- Created: 2026-06-21
- Updated: 2026-06-21

## Summary

Exercise the DLQ, status topic, and at-least-once delivery semantics against a real Kafka broker via Testcontainers, going beyond the broker-free unit/simulation tests used elsewhere in the module, and fix a flaky Windows `@TempDir` cleanup issue surfaced by the IT suite.

## Problem

Broker-free unit tests and in-memory simulations validate logic but cannot fully confirm behavior against a real Kafka broker (offset semantics, consumer-group fan-out, topic discovery). A real-broker IT suite was needed, along with a fix for flaky `@TempDir` cleanup observed on Windows during `CoordinatorLifecycleIntegrationTest`.

## Acceptance criteria

- [x] `DlqManagerIT` (Testcontainers `apache/kafka:3.8.1`): list/count/topicStats correctness, requeue (with and without delay), discard-vs-requeue distinction, multi-stage DLQ aggregation across topics. 8+ broker-backed tests.
- [x] `StatusTopicIT`: multi-consumer-group fan-out, `agentId` stamping via the 2-arg vs 1-arg producer constructors, all 7 major event types surviving JSON serialization through Kafka, `fromBeginning=true` replay rebuilding `CaseCompletionMonitor` state, consistent partition-hash-key contract for a given `caseId`. 5 broker-backed tests.
- [x] `AtLeastOnceDeliveryIT`: commit-watermark-only redelivery, gap-stop redelivery, all-committed no-redelivery — against a real broker, not just in-memory simulation. 3 broker-backed tests.
- [x] Fixed flaky `@TempDir` cleanup on Windows: `CaseLifecycleManager.persistState()`/`loadState()` switched to `mapper.writeValueAsBytes()`+`Files.write()`/`mapper.readValue(Files.readAllBytes())` (NIO, closes handle before rename) instead of `mapper.writeValue(File)`.
- [x] All IT tests skip gracefully when Docker is unavailable (`disabledWithoutDocker = true`); IT suite: 22 tests, all skipped without Docker; full unit suite: 514 tests, 0 failures, stable across repeated runs.

## Updates

### 2026-06-21
- Issue created during roadmap consolidation, extracted from `iped-distributed-ROADMAP.md` (Phase 9), grouping all Testcontainers IT additions and the Windows tempdir flakiness fix into one issue. Status set to `done`.
