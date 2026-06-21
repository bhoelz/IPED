# ISSUE-424: Retire the SPA WorkspacePage in iped-webui

- Status: done
- Roadmap: [iped-webui-server-ROADMAP.md](../roadmaps/iped-webui-server-ROADMAP.md)
- Roadmap section: Phase 2 — Island growth
- Owner: unassigned
- Created: 2026-06-21
- Updated: 2026-06-21

## Summary

`src/app/domains/` (12 files) was deleted in `iped-webui`, and `@angular/router`/`@angular/forms` were removed from `package.json`, with `app.ts` and `app.config.ts` stripped of router imports, completing the retirement of the legacy SPA shell once islands covered its workflows.

## Problem

Once islands covered the workspace's workflows, the legacy SPA shell in `iped-webui` was redundant and needed removal to avoid maintaining two parallel implementations.

## Acceptance criteria

- [x] `src/app/domains/` deleted from `iped-webui`.
- [x] `@angular/router`/`@angular/forms` removed from `iped-webui`'s `package.json`.

## Updates

### 2026-06-21
- Issue created during roadmap consolidation, extracted from `iped-webui-server-ROADMAP.md`, status set to `done` based on the original `[x]` marker. This duplicates work tracked in `iped-webui-ROADMAP.md` Phase 3 (ISSUE-402 through ISSUE-404); both roadmaps refer to the same completed retirement.
