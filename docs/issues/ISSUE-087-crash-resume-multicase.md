# ISSUE-087: Crash/resume correctness with multiple active cases

- Status: done
- Roadmap: [iped-engine-ROADMAP.md](../roadmaps/iped-engine-ROADMAP.md)
- Roadmap section: Phase 2 — Multi-case hardening
- Owner: unassigned
- Created: 2026-06-21
- Updated: 2026-06-21

## Summary

Verify `SaveStateThread` per-case queues remain correctly isolated under crash/failure injection with multiple active cases.

## Problem

Crash/resume logic needed validation to ensure per-case queues don't bleed state across cases when failures are injected during concurrent multi-case processing.

## Acceptance criteria

- [x] Audit confirms `SaveStateThread` uses `ConcurrentHashMap<UUID, Queue<...>>` keyed by case UUID, with per-case state correctly isolated (no code change required).

## Updates

### 2026-06-21
- Issue created during roadmap consolidation, extracted from `iped-engine-ROADMAP.md`, status set to `done`.
