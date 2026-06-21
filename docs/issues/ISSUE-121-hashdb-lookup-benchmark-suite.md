# ISSUE-121: Benchmark suite for hash lookup throughput

- Status: planned
- Roadmap: [iped-engine-hashdb-ROADMAP.md](../roadmaps/iped-engine-hashdb-ROADMAP.md)
- Roadmap section: Phase 2 — Performance and formats
- Owner: unassigned
- Created: 2026-06-21
- Updated: 2026-06-21

## Summary

A benchmark suite measuring lookups/sec at 1M, 100M, and 1B entry scales is needed to protect against performance regressions in the hash database lookup path.

## Problem

There is no repeatable benchmark today that would catch a throughput regression in hash lookups before it reaches a release.

## Acceptance criteria

- [ ] Benchmark measures lookups/sec at 1M, 100M, and 1B entry database sizes.
- [ ] Benchmark can be run repeatably (e.g. as part of release validation).

## Updates

### 2026-06-21
- Issue created during roadmap consolidation, extracted from `iped-engine-hashdb-ROADMAP.md`, status set to `planned` based on the original `[ ]` marker.
