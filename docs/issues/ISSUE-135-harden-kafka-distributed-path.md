# ISSUE-135: Harden the Kafka distributed path to production-grade

- Status: in_progress
- Roadmap: [iped-engine-parent-ROADMAP.md](../roadmaps/iped-engine-parent-ROADMAP.md)
- Roadmap section: Phase 3 — Multi-case and distributed maturity
- Owner: unassigned
- Created: 2026-06-21
- Updated: 2026-06-21

## Summary

Harden the Kafka distributed processing path (`iped-distributed`) to production grade per workstream 4 of the root `ROADMAP.md`.

## Problem

This is the cross-cutting tracking item for the entire `iped-distributed` hardening effort, which has its own detailed roadmap (`iped-distributed-ROADMAP.md`) covering correctness guarantees, operability, scale/scheduling, production rollout, and resilience validation.

## Acceptance criteria

- [ ] See `iped-distributed-ROADMAP.md` for the full breakdown of correctness, operability, scaling, security, and resilience issues (ISSUE-057 through ISSUE-081).
- [ ] Confirm all Phase 1-9 items in `iped-distributed-ROADMAP.md` reach `done` before closing this cross-cutting item.

## Updates

### 2026-06-21
- Issue created during roadmap consolidation, extracted from `iped-engine-parent-ROADMAP.md`, status set to `in_progress` (most of `iped-distributed-ROADMAP.md`'s phases are already done, per that module's roadmap).
