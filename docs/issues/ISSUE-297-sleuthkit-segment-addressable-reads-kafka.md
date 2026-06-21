# ISSUE-297: Segment-addressable reads for Kafka distributed pipeline

- Status: planned
- Roadmap: [iped-sleuthkit-ROADMAP.md](../roadmaps/iped-sleuthkit-ROADMAP.md)
- Roadmap section: Phase 3 — Distributed processing
- Owner: unassigned
- Created: 2026-06-21
- Updated: 2026-06-21

## Summary

Distributed agents must be able to open a sub-range of a disk image independently — with the image accessible via shared storage and offsets/inode lists used as work units — to support the Kafka distributed processing pipeline.

## Problem

The Sleuthkit reader currently has no concept of independently addressable sub-ranges of an image, which the distributed pipeline needs to split work across agents.

## Acceptance criteria

- [ ] An agent can open and read a defined sub-range (offset/inode-list based) of an image independently.
- [ ] Image is accessible via shared storage for all participating agents.

## Updates

### 2026-06-21
- Issue created during roadmap consolidation, extracted from `iped-sleuthkit-ROADMAP.md`, status set to `planned` based on the original `[ ]` marker.
