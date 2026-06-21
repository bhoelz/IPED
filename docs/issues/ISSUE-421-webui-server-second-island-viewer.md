# ISSUE-421: Second island: iped-viewer wired into the workspace

- Status: done
- Roadmap: [iped-webui-server-ROADMAP.md](../roadmaps/iped-webui-server-ROADMAP.md)
- Roadmap section: Phase 2 — Island growth
- Owner: unassigned
- Created: 2026-06-21
- Updated: 2026-06-21

## Summary

`viewer.rocker.html` was updated to use `<iped-viewer item-id media-type api-base>`; `WorkspaceFragmentController.viewer()` now fetches metadata for both "meta" and "preview" modes to extract `mediaType`, and `fetchBookmarks()` was updated to call the v2 bookmarks endpoint.

## Problem

The SSR workspace needed to host the viewer island (built in `iped-webui`) and supply it the metadata it needs to route by MIME type.

## Acceptance criteria

- [x] `viewer.rocker.html` embeds `<iped-viewer>` with the required attributes.
- [x] `WorkspaceFragmentController.viewer()` fetches and passes `mediaType` for both modes.
- [x] `fetchBookmarks()` calls `GET /v2/bookmarks`.

## Updates

### 2026-06-21
- Issue created during roadmap consolidation, extracted from `iped-webui-server-ROADMAP.md`, status set to `done` based on the original `[x]` marker.
