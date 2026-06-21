# ISSUE-248: PayloadSigner HMAC-SHA256 for chain-of-custody integrity

- Status: done
- Roadmap: [iped-runner-ROADMAP.md](../roadmaps/iped-runner-ROADMAP.md)
- Roadmap section: Phase 5 — Kafka security and chain-of-custody audit
- Owner: unassigned
- Created: 2026-06-21
- Updated: 2026-06-21

## Summary

`PayloadSigner` computes an HMAC-SHA256 over a canonical string (`iped-v1|caseId|itemUuid|pipelineStage|path|lengthBytes`) with constant-time verification, guarded by `isEnabled(secret)` for a rolling upgrade path.

## Problem

Distributed processing messages needed tamper-evidence to support chain-of-custody guarantees across agents.

## Acceptance criteria

- [x] `sign()`/`verify()` static methods compute/validate the HMAC-SHA256 signature with constant-time comparison.
- [x] `isEnabled(secret)` guard supports rolling upgrades without breaking older agents.

## Updates

### 2026-06-21
- Issue created during roadmap consolidation, extracted from `iped-runner-ROADMAP.md`, status set to `done` based on the original `[x]` marker.
