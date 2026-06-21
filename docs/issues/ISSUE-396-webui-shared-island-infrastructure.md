# ISSUE-396: Shared island infrastructure (attribute/event contracts, API client, design tokens)

- Status: done
- Roadmap: [iped-webui-ROADMAP.md](../roadmaps/iped-webui-ROADMAP.md)
- Roadmap section: Phase 1 — Island library consolidation
- Owner: unassigned
- Created: 2026-06-21
- Updated: 2026-06-21

## Summary

`src/islands/shared/` was created with `api.ts` (`API_BASE_URL` DI token), `events.ts` (typed `CustomEvent` factories: `itemSelectedEvent`, `selectionChangedEvent`, `resultsLoadedEvent`, `islandErrorEvent`), and `island-base.ts` (abstract `IslandBase` directive with `hostEl`, `apiBase`, and `dispatch<T>()`).

## Problem

Each island needed shared, typed infrastructure for API access, cross-island events, and design tokens via CSS variables, rather than reinventing these per island.

## Acceptance criteria

- [x] `api.ts` provides an `API_BASE_URL` DI token, overridable via an `api-base` attribute.
- [x] `events.ts` defines typed `CustomEvent` factories with detail interfaces, `bubbles`/`composed: true`.
- [x] `island-base.ts` provides an `IslandBase` directive used by `ResultsGridComponent`.

## Updates

### 2026-06-21
- Issue created during roadmap consolidation, extracted from `iped-webui-ROADMAP.md`, status set to `done` based on the original `[x]` marker.
