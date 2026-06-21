# ISSUE-298: Read-throughput benchmark to size distributed work units

- Status: planned
- Roadmap: [iped-sleuthkit-ROADMAP.md](../roadmaps/iped-sleuthkit-ROADMAP.md)
- Roadmap section: Phase 3 — Distributed processing
- Owner: unassigned
- Created: 2026-06-21
- Updated: 2026-06-21

## Summary

A read-throughput benchmark for the Sleuthkit reader is needed to inform how large distributed work units (segment ranges) should be sized for the Kafka pipeline.

## Problem

Without throughput data, there's no empirical basis for choosing distributed work-unit sizes for disk image processing.

## Acceptance criteria

- [ ] Benchmark measures read throughput for representative image types/sizes.
- [ ] Results are used to define a recommended work-unit sizing guideline.

## Updates

### 2026-06-21
- Issue created during roadmap consolidation, extracted from `iped-sleuthkit-ROADMAP.md`, status set to `planned` based on the original `[ ]` marker.
