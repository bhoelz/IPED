# ISSUE-420: Error/empty/loading states for every fragment when backend is down

- Status: done
- Roadmap: [iped-webui-server-ROADMAP.md](../roadmaps/iped-webui-server-ROADMAP.md)
- Roadmap section: Phase 1 — Real data end-to-end
- Owner: unassigned
- Created: 2026-06-21
- Updated: 2026-06-21

## Summary

`FetchResult<T>(data, fromBackend)` distinguishes live vs demo data; sidebar shows a `backendUnavailableBanner()` when `fromBackend=false`, the viewer meta tab shows an `itemErrorFragment()`, and `SearchBffController` returns structured 503/502 JSON so the island grid can render a clean "backend unavailable" state. `IslandManifest` was also updated to read `manifest.json` for per-island chunk lookups, with `pom.xml` copying it alongside the browser bundles.

## Problem

When the backend proxy returned errors, fragments needed a clean, surfaced error/empty state rather than silently showing an empty list or broken markup.

## Acceptance criteria

- [x] `FetchResult<T>` wrapper distinguishes live vs demo data across fragments.
- [x] Sidebar shows `backendUnavailableBanner()` when data isn't from the backend.
- [x] Viewer meta tab shows `itemErrorFragment()` on metadata fetch failure.
- [x] `SearchBffController` returns structured 503/502 JSON error bodies.
- [x] `IslandManifest` reads `manifest.json` for per-island chunk lookups.

## Updates

### 2026-06-21
- Issue created during roadmap consolidation, extracted from `iped-webui-server-ROADMAP.md`, status set to `done` based on the original `[x]` marker.
