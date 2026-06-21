# ISSUE-014: Time-series store connector for temporal analytics

- Status: planned
- Roadmap: [iped-additional-index-ROADMAP.md](../roadmaps/iped-additional-index-ROADMAP.md)
- Roadmap section: Phase 2 — First-class connectors
- Owner: unassigned
- Created: 2026-06-21
- Updated: 2026-06-21

## Summary

A time-series store connector is needed for temporal analytics, in particular timeline events, built on top of the connector contract defined in Phase 1.

## Problem

There is no dedicated connector for storing and querying timeline/temporal event data outside the main Lucene index, limiting temporal analytics capability.

## Acceptance criteria

- [ ] Time-series connector implemented behind the connector SPI.
- [ ] Timeline events can be indexed and queried through it.

## Updates

### 2026-06-21
- Issue created during roadmap consolidation, extracted from `iped-additional-index-ROADMAP.md`, status set to `planned` based on the original `[ ]` marker.
