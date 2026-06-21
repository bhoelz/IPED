# ISSUE-088: Validate per-case resource quotas with large real cases

- Status: blocked
- Roadmap: [iped-engine-ROADMAP.md](../roadmaps/iped-engine-ROADMAP.md)
- Roadmap section: Phase 2 — Multi-case hardening
- Owner: unassigned
- Created: 2026-06-21
- Updated: 2026-06-21

## Summary

Validate `ResourceManager`'s per-case resource quota enforcement against large real-world evidence sets, beyond unit-test scale.

## Problem

`ResourceManager` is already implemented per-case, but its behavior under large, real multi-GB evidence sets has not been validated. This requires dedicated lab infrastructure that is not available within this branch's scope.

## Acceptance criteria

- [ ] Stand up lab infrastructure capable of processing multi-GB real evidence cases.
- [ ] Validate per-case `ResourceManager` quota enforcement under that load.

## Updates

### 2026-06-21
- Issue created during roadmap consolidation, extracted from `iped-engine-ROADMAP.md`. Status set to `blocked` — deferred to a follow-up task requiring dedicated lab infrastructure outside this branch's scope.
