# ISSUE-120: Graceful disable of hash lookup tasks when hashdb config missing

- Status: planned
- Roadmap: [iped-engine-hashdb-ROADMAP.md](../roadmaps/iped-engine-hashdb-ROADMAP.md)
- Roadmap section: Phase 1 — Module boundary
- Owner: unassigned
- Created: 2026-06-21
- Updated: 2026-06-21

## Summary

The hashdb module should be optional at runtime: when there is no hash DB configuration present, dependent lookup tasks should disable themselves gracefully rather than failing.

## Problem

It isn't yet verified that the engine handles a missing hashdb configuration cleanly instead of erroring out lookup-dependent tasks.

## Acceptance criteria

- [ ] Missing hashdb config disables lookup tasks without failing the case.
- [ ] A clear log message indicates lookups are disabled.

## Updates

### 2026-06-21
- Issue created during roadmap consolidation, extracted from `iped-engine-hashdb-ROADMAP.md`, status set to `planned` based on the original `[ ]` marker.
