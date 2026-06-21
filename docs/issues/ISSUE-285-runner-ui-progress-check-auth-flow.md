# ISSUE-285: Progress check — auth key flow verified end-to-end

- Status: done
- Roadmap: [iped-runner-ui-ROADMAP.md](../roadmaps/iped-runner-ui-ROADMAP.md)
- Roadmap section: Progress checks
- Owner: unassigned
- Created: 2026-06-21
- Updated: 2026-06-21

## Summary

Verified that the API key is stored in `sessionStorage` and that a 401 response surfaces `ApiKeyModal` from any page in the app.

## Problem

The auth UX needed verification across the whole app, not just the screens where it was originally implemented.

## Acceptance criteria

- [x] API key persists in `sessionStorage`.
- [x] A 401 from any page surfaces `ApiKeyModal`.

## Updates

### 2026-06-21
- Issue created during roadmap consolidation, extracted from `iped-runner-ui-ROADMAP.md`, status set to `done` based on the original `[x]` marker.
