# ISSUE-402: Remove SPA build/serve architect targets from angular.json

- Status: done
- Roadmap: [iped-webui-ROADMAP.md](../roadmaps/iped-webui-ROADMAP.md)
- Roadmap section: Phase 3 — SPA retirement
- Owner: unassigned
- Created: 2026-06-21
- Updated: 2026-06-21

## Summary

`npm run build` no longer builds the SPA shell; only `build:islands`/`build:islands:dev` are active, and `src/app/app.routes.ts` was replaced with an empty routes array, removing the broken lazy-load of `WorkspacePage`.

## Problem

The SPA build targets were dead weight once the islands architecture took over, and the broken `WorkspacePage` lazy-load route needed to be removed.

## Acceptance criteria

- [x] SPA `build`/`serve` architect targets removed from `angular.json`.
- [x] `app.routes.ts` is an empty routes array.

## Updates

### 2026-06-21
- Issue created during roadmap consolidation, extracted from `iped-webui-ROADMAP.md`, status set to `done` based on the original `[x]` marker.
