# ISSUE-352: Keep iped-utils at zero dependencies on other IPED modules

- Status: planned
- Roadmap: [iped-utils-ROADMAP.md](../roadmaps/iped-utils-ROADMAP.md)
- Roadmap section: Phase 3 — Long-term shape
- Owner: unassigned
- Created: 2026-06-21
- Updated: 2026-06-21

## Summary

Keep `iped-utils` free of dependencies on other IPED modules and enforce that with an ArchUnit rule, since it sits below `iped-api` in the dependency stack.

## Problem

Without an automated check, `iped-utils` could accidentally acquire a dependency on a higher-level IPED module, breaking its role as a dependency-light foundation module.

## Acceptance criteria

- [ ] ArchUnit rule added forbidding `iped-utils` dependencies on other IPED modules.

## Updates

### 2026-06-21
- Issue created during roadmap consolidation, extracted from `iped-utils-ROADMAP.md`, status set to `planned` based on the original `[ ]` marker.
