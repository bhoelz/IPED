# ISSUE-408: viewer.component.spec.ts test coverage

- Status: done
- Roadmap: [iped-webui-ROADMAP.md](../roadmaps/iped-webui-ROADMAP.md)
- Roadmap section: Phase 5 — Viewer island and event contract extension
- Owner: unassigned
- Created: 2026-06-21
- Updated: 2026-06-21

## Summary

14 tests cover empty state, text/html fetch with `Accept` header, highlight query param, viewer-ready dispatch, image/pdf no-fetch behavior, hex fallback for binary/empty MIME, error state plus `island-error` dispatch, and the viewerType MIME routing table for 10 MIME types.

## Problem

The viewer-host island's MIME-routing logic and event dispatch behavior needed dedicated test coverage given its central role in the application.

## Acceptance criteria

- [x] 14 tests cover empty state, fetch behavior per mode, viewer-ready dispatch, error states, and MIME routing for 10 types.

## Updates

### 2026-06-21
- Issue created during roadmap consolidation, extracted from `iped-webui-ROADMAP.md`, status set to `done` based on the original `[x]` marker.
