# ISSUE-446: Fix DOM event-handler XSS in Rocker templates (onclick string interpolation)

- Status: planned
- Roadmap: [iped-webui-server-ROADMAP.md](../roadmaps/iped-webui-server-ROADMAP.md)
- Roadmap section: Phase 3 — Auth, sessions, hardening
- Owner: unassigned
- Created: 2026-06-21
- Updated: 2026-06-21

## Summary

Untrusted values (item id/name, case id, category/bookmark name) are interpolated into JavaScript string literals inside inline `onclick="..."` handlers in several Rocker fragments. Rocker's HTML-entity escaping is insufficient here because the browser decodes entities back to raw characters before the JS parser runs, so a `'` in a name breaks out of the string literal.

## Problem

Confirmed still present (verified 2026-06-21) in:
- `itemList.rocker.html:55` — `onclick="ipedSelectItem('@r.iid()')"`
- `sidebar.rocker.html:52,70,93` — `ipedSelectFilter`/`ipedEvidToggle` handlers interpolating `@cat.name()`, `@caseId`, `@bm.name()`
- `evidenceChildren.rocker.html:8` — `ipedEvidToggle('@r.id()', '@caseId')`

## Acceptance criteria

- [ ] Replace inline `onclick="ipedSelectItem('@r.iid()')"` (and the other interpolated handlers above) with `data-*` attributes plus one delegated `click` listener per action, e.g. `data-iid="@r.iid()"` read via `event.target.closest(...).dataset`.
- [ ] Apply the same pattern to `ipedSelectFilter`, `ipedEvidToggle`, `ipedAiToggle`.
- [ ] Add a regression test: injecting `'-alert(1)-'` as an item/category/bookmark name or `caseId` no longer executes; the value renders inert.

## Updates

### 2026-06-21
- Issue created while triaging `docs/needs-revision/SECURITY-FIX-PLAN.md` (originally "Task 1", High severity). Re-verified against current code: the vulnerable interpolation patterns are still present unchanged, so this stays `planned`/open — unlike the plan's Task 0/2/3, which were confirmed already resolved by later work (auth/CSRF baseline, viewer sanitizer, export endpoint rewrite).
