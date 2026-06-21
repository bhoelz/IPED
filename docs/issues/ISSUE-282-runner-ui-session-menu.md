# ISSUE-282: Session menu with API-key status

- Status: done
- Roadmap: [iped-runner-ui-ROADMAP.md](../roadmaps/iped-runner-ui-ROADMAP.md)
- Roadmap section: Phase 5 — Operator UX polish
- Owner: unassigned
- Created: 2026-06-21
- Updated: 2026-06-21

## Summary

The user icon was replaced with a `SessionMenu` component showing a green/muted `key-dot` badge; its dropdown exposes API-key status, "Change key" (opens `ApiKeyModal`), and "Sign out" (clears sessionStorage and `hasKey` state), with `hasKey` synced on connect.

## Problem

There was no visible indicator of whether an API key was set, nor a clean way to change or clear it from the UI.

## Acceptance criteria

- [x] `SessionMenu` shows a key-status badge.
- [x] Dropdown offers "Change key" and "Sign out".
- [x] `hasKey` state is synced on connect.

## Updates

### 2026-06-21
- Issue created during roadmap consolidation, extracted from `iped-runner-ui-ROADMAP.md`, status set to `done` based on the original `[x]` marker.
