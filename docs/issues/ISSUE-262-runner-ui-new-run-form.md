# ISSUE-262: New-run form with path picker and profile selector

- Status: done
- Roadmap: [iped-runner-ui-ROADMAP.md](../roadmaps/iped-runner-ui-ROADMAP.md)
- Roadmap section: Phase 2 — Launch and queue management
- Owner: unassigned
- Created: 2026-06-21
- Updated: 2026-06-21

## Summary

The existing config-builder screens cover evidence path picking and profile selection; the `RunModal` review phase adds a priority selector (HIGH/NORMAL/LOW) that is sent with the `POST /run` body, with the server profile list fetched from `GET /profiles` on app mount.

## Problem

Launching a run needed pre-launch validation feedback and a way to choose priority and profile from the UI.

## Acceptance criteria

- [x] `RunModal` review phase includes a priority selector.
- [x] Chosen priority is sent in the `POST /run` body.
- [x] Server profile list is fetched from `GET /profiles` on app mount.

## Updates

### 2026-06-21
- Issue created during roadmap consolidation, extracted from `iped-runner-ui-ROADMAP.md`, status set to `done` based on the original `[x]` marker.
