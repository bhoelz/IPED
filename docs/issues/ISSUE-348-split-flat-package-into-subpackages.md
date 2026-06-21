# ISSUE-348: Split the flat iped.utils package into cohesive subpackages

- Status: planned
- Roadmap: [iped-utils-ROADMAP.md](../roadmaps/iped-utils-ROADMAP.md)
- Roadmap section: Phase 2 — Structure and quality
- Owner: unassigned
- Created: 2026-06-21
- Updated: 2026-06-21

## Summary

Split the flat `iped.utils` package into cohesive subpackages (`io`, `text`, `image`, `fs`, `concurrent`) without breaking binary compatibility in the 4.x line, using deprecated forwarders for any moved classes.

## Problem

A single flat package mixing IO, text, image, and filesystem helpers makes the module harder to navigate and reason about; restructuring needs to preserve binary compatibility for 4.x consumers.

## Acceptance criteria

- [ ] `iped.utils` split into `io`, `text`, `image`, `fs`, `concurrent` subpackages.
- [ ] Binary compatibility preserved in 4.x via deprecated forwarders for moved classes.

## Updates

### 2026-06-21
- Issue created during roadmap consolidation, extracted from `iped-utils-ROADMAP.md`, status set to `planned` based on the original `[ ]` marker.
