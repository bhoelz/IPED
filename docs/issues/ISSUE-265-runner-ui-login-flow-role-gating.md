# ISSUE-265: Login flow and role-gated actions once runner auth lands

- Status: done
- Roadmap: [iped-runner-ui-ROADMAP.md](../roadmaps/iped-runner-ui-ROADMAP.md)
- Roadmap section: Phase 3 — Hardening
- Owner: unassigned
- Created: 2026-06-21
- Updated: 2026-06-21

## Summary

The `apiFetch` wrapper in `api.js` attaches `X-Api-Key` from `sessionStorage`; 401 responses clear the key and surface `ApiKeyModal` via a module-level `setUnauthorizedHandler`. All protected fetches (RunModal, DashboardView, profiles) go through `apiFetch`.

## Problem

Once the runner API required an API key, the frontend needed a consistent way to attach it, detect expiry/invalidity, and prompt the user to re-enter it.

## Acceptance criteria

- [x] `apiFetch` wrapper attaches `X-Api-Key` from `sessionStorage`.
- [x] 401 responses clear the stored key and surface `ApiKeyModal`.
- [x] All protected fetches route through `apiFetch`.

## Updates

### 2026-06-21
- Issue created during roadmap consolidation, extracted from `iped-runner-ui-ROADMAP.md`, status set to `done` based on the original `[x]` marker.
