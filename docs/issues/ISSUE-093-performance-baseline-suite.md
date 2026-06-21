# ISSUE-093: Performance baseline suite recorded per release

- Status: blocked
- Roadmap: [iped-engine-ROADMAP.md](../roadmaps/iped-engine-ROADMAP.md)
- Roadmap section: Phase 4 — 5.0 platform role
- Owner: unassigned
- Created: 2026-06-21
- Updated: 2026-06-21

## Summary

Record an items/sec and index-throughput performance baseline per release so distributed and multi-case changes can prove no single-case regression.

## Problem

There is no benchmark suite today to detect single-case performance regressions introduced by distributed or multi-case changes. This requires dedicated benchmark infrastructure and representative evidence sets that are not yet available.

## Acceptance criteria

- [ ] Add JMH dependency and a skeleton benchmark class.
- [ ] Define representative evidence sets for benchmarking.
- [ ] Record and publish a per-release performance baseline (items/sec, index throughput).

## Updates

### 2026-06-21
- Issue created during roadmap consolidation, extracted from `iped-engine-ROADMAP.md`. Status set to `blocked` — deferred pending dedicated benchmark infrastructure and representative evidence sets.
