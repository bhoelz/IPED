# ISSUE-349: Bring unit-test coverage to all non-trivial utilities

- Status: planned
- Roadmap: [iped-utils-ROADMAP.md](../roadmaps/iped-utils-ROADMAP.md)
- Roadmap section: Phase 2 — Structure and quality
- Owner: unassigned
- Created: 2026-06-21
- Updated: 2026-06-21

## Summary

Add unit-test coverage for all non-trivial utilities in `iped.utils`, since they are the most-reused and least-tested code in the project.

## Problem

Utility classes are depended on by nearly every module in the reactor, yet currently have disproportionately low test coverage relative to how widely they are reused, creating outsized regression risk.

## Acceptance criteria

- [ ] Non-trivial utilities in `iped.utils` identified.
- [ ] Unit tests added covering their behavior.

## Updates

### 2026-06-21
- Issue created during roadmap consolidation, extracted from `iped-utils-ROADMAP.md`, status set to `planned` based on the original `[ ]` marker.
