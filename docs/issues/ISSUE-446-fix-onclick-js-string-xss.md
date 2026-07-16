# ISSUE-446: Fix DOM event-handler XSS in Rocker templates (onclick string interpolation)

- Status: done
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

- [x] Replace inline interpolated handlers with `data-*` attributes and delegated click handling.
- [x] Apply the same pattern to `ipedSelectFilter`, `ipedEvidToggle`, and `ipedAiToggle`.
- [x] Add workspace regression coverage ensuring vulnerable interpolations are absent.

## Updates

### 2026-07-16
- Replaced vulnerable interpolated handlers with delegated `data-*` actions
  and added workspace regression coverage.

### 2026-06-21
- Issue created while triaging `docs/needs-revision/SECURITY-FIX-PLAN.md` (originally "Task 1", High severity). Re-verified against current code: the vulnerable interpolation patterns are still present unchanged, so this stays `planned`/open — unlike the plan's Task 0/2/3, which were confirmed already resolved by later work (auth/CSRF baseline, viewer sanitizer, export endpoint rewrite).
