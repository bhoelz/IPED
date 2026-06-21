# ISSUE-403: Delete src/app/domains/ tree

- Status: done
- Roadmap: [iped-webui-ROADMAP.md](../roadmaps/iped-webui-ROADMAP.md)
- Roadmap section: Phase 3 — SPA retirement
- Owner: unassigned
- Created: 2026-06-21
- Updated: 2026-06-21

## Summary

`git rm -r iped-webui/src/app/domains` removed 12 files — item/jobs/search/selection/session/viewer facades plus the workspace-page component — confirmed dead code with no remaining importers.

## Problem

The legacy SPA domains tree had no remaining importers once islands took over its functionality, and was left as dead code.

## Acceptance criteria

- [x] `src/app/domains/` tree deleted (12 files removed).
- [x] No remaining importers reference the deleted tree.

## Updates

### 2026-06-21
- Issue created during roadmap consolidation, extracted from `iped-webui-ROADMAP.md`, status set to `done` based on the original `[x]` marker.
