# ISSUE-395: Restructure src/ around islands as the primary artifact

- Status: done
- Roadmap: [iped-webui-ROADMAP.md](../roadmaps/iped-webui-ROADMAP.md)
- Roadmap section: Phase 1 — Island library consolidation
- Owner: unassigned
- Created: 2026-06-21
- Updated: 2026-06-21

## Summary

`src/islands/` is now the canonical location for all island components; the SPA shell (`WorkspacePage`, domain modules) remains but is frozen — no new features added there.

## Problem

The workspace needed a clear, primary location for island code, separate from the legacy SPA shell that was being phased out.

## Acceptance criteria

- [x] `src/islands/*` established as the primary artifact location.
- [x] SPA shell marked as legacy/frozen — no new features added to it.

## Updates

### 2026-06-21
- Issue created during roadmap consolidation, extracted from `iped-webui-ROADMAP.md`, status set to `done` based on the original `[x]` marker.
