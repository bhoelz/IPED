# ISSUE-024: Mark internal-but-public types with an `@Internal` annotation

- Status: done
- Roadmap: [iped-api-ROADMAP.md](../roadmaps/iped-api-ROADMAP.md)
- Roadmap section: Phase 1 — Contract hygiene
- Owner: unassigned
- Created: 2026-06-21
- Updated: 2026-06-21

## Summary

Introduce a clear annotation for types that must be `public` for technical reasons but are not part of the supported scripting API surface, so generated scripting docs can exclude them.

## Problem

Without a marker, scripting documentation generation cannot distinguish genuinely public API types from internal-but-public implementation details, polluting the generated docs.

## Acceptance criteria

- [x] `iped.annotation.Internal` annotation added.
- [x] Internal-but-public types annotated so doc generation can exclude them.

## Updates

### 2026-06-21
- Issue created during roadmap consolidation, extracted from `iped-api-ROADMAP.md`, status set to `done` based on the original `[x]` marker.
