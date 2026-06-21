# ISSUE-071: Config validation at coordinator startup

- Status: done
- Roadmap: [iped-distributed-ROADMAP.md](../roadmaps/iped-distributed-ROADMAP.md)
- Roadmap section: Phase 5 — Production hardening
- Owner: unassigned
- Created: 2026-06-21
- Updated: 2026-06-21

## Summary

Validate `DistributedConfig` at coordinator startup and fail fast with actionable error messages instead of failing unpredictably mid-run.

## Problem

Misconfigured deployments (blank required strings, invalid ports, inconsistent timeout/threshold pairs) could otherwise fail confusingly deep into a processing run rather than immediately at startup.

## Acceptance criteria

- [x] `ConfigValidator.validate(DistributedConfig)` returns a list of actionable error messages.
- [x] Rules cover blank required strings (`kafkaBootstrapServers`, `coordinatorServerUrl`, `sharedStorageRoot`), port range, positive timeouts, heartbeat < expiry invariant, partitions/replication/parallelism >= 1, work-unit oversized >= target, backpressure heap soft < hard, disk hard < soft, SASL username required when mechanism is set, keystore password required when path is set.
- [x] Wired into `CoordinatorServer.main()`; callers fail-fast at startup.
- [x] 15 broker-free tests in `Phase5HardeningTest`.

## Updates

### 2026-06-21
- Issue created during roadmap consolidation, extracted from `iped-distributed-ROADMAP.md` (Phase 5), status set to `done`.
