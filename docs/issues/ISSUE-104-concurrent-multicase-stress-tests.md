# ISSUE-104: Stress-test search/data primitives under concurrent multi-case access

- Status: done
- Roadmap: [iped-engine-core-ROADMAP.md](../roadmaps/iped-engine-core-ROADMAP.md)
- Roadmap section: Phase 3 — Multi-case readiness
- Owner: unassigned
- Created: 2026-06-21
- Updated: 2026-06-21

## Summary

Make search/data primitives safe under concurrent multi-case access and add stress tests for shared caches.

## Problem

Shared caches in search/data primitives needed concurrency stress testing to confirm isolation between simultaneously running cases.

## Acceptance criteria

- [x] `ConfigurationManagerConcurrentCaseTest` added: 20 concurrent case instances, each with an isolated `SentinelConfig`, verifying no cross-contamination.
- [x] `createCaseInstanceNeverReturnsSingleton` test added.
- [x] `caseInstanceIsolatedFromSingleton` test added.

## Updates

### 2026-06-21
- Issue created during roadmap consolidation, extracted from `iped-engine-core-ROADMAP.md`, status set to `done`.
