# ISSUE-404: Remove dead SPA dependencies from package.json

- Status: done
- Roadmap: [iped-webui-ROADMAP.md](../roadmaps/iped-webui-ROADMAP.md)
- Roadmap section: Phase 3 — SPA retirement
- Owner: unassigned
- Created: 2026-06-21
- Updated: 2026-06-21

## Summary

`@angular/router` and `@angular/forms` were removed from dependencies once the domains tree was deleted; `app.ts` no longer imports `RouterOutlet`, and `app.config.ts` no longer calls `provideRouter`.

## Problem

Once routing-dependent SPA code was removed, the router and forms packages became unused by any island and were dead weight in the dependency tree.

## Acceptance criteria

- [x] `@angular/router` and `@angular/forms` removed from `package.json` dependencies.
- [x] `app.ts` and `app.config.ts` no longer reference router APIs.

## Updates

### 2026-06-21
- Issue created during roadmap consolidation, extracted from `iped-webui-ROADMAP.md`, status set to `done` based on the original `[x]` marker.
