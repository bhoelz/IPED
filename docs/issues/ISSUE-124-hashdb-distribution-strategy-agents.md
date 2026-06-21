# ISSUE-124: Hash DB distribution strategy for distributed agents

- Status: planned
- Roadmap: [iped-engine-hashdb-ROADMAP.md](../roadmaps/iped-engine-hashdb-ROADMAP.md)
- Roadmap section: Phase 3 — Distributed processing
- Owner: unassigned
- Created: 2026-06-21
- Updated: 2026-06-21

## Summary

Distributed agents need a defined strategy for accessing the hash database — either a shared network copy or a per-agent local replica — with the supported topologies documented and configuration options provided for each.

## Problem

There is currently no documented or configurable approach for how distributed processing agents should obtain access to the hash database.

## Acceptance criteria

- [ ] Shared-network-copy and per-agent-local-replica topologies are both documented.
- [ ] Configuration exists to select between the supported topologies.

## Updates

### 2026-06-21
- Issue created during roadmap consolidation, extracted from `iped-engine-hashdb-ROADMAP.md`, status set to `planned` based on the original `[ ]` marker.
