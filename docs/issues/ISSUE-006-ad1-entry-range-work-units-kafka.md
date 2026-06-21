# ISSUE-006: Expose AD1 entry-range work units for the Kafka pipeline

- Status: planned
- Roadmap: [iped-ad1-ROADMAP.md](../roadmaps/iped-ad1-ROADMAP.md)
- Roadmap section: Phase 3 — Distributed processing
- Owner: unassigned
- Created: 2026-06-21
- Updated: 2026-06-21

## Summary

AD1 entries are independently addressable, making this module a good first candidate for distributed datasource support. Entry ranges should be exposed as work units consumable by the Kafka-based distributed processing pipeline.

## Problem

The distributed processing pipeline needs a way to split AD1 evidence into independently processable work units; this capability does not yet exist for AD1.

## Acceptance criteria

- [ ] AD1 entries can be addressed by range and exposed as discrete work units.
- [ ] Work units integrate with the Kafka distributed pipeline's work-distribution model.

## Updates

### 2026-06-21
- Issue created during roadmap consolidation, extracted from `iped-ad1-ROADMAP.md`, status set to `planned` based on the original `[ ]` marker.
