# ISSUE-035: Adopt feature freeze policy for the Swing analysis UI

- Status: done
- Roadmap: [iped-app-ROADMAP.md](../roadmaps/iped-app-ROADMAP.md)
- Roadmap section: Phase 2 — Maintenance mode for Swing analysis UI
- Owner: unassigned
- Created: 2026-06-21
- Updated: 2026-06-21

## Summary

Formalize that the Swing analysis UI is in maintenance mode: bug fixes and viewer parity support only, with new analyst features targeting the browser UI first. Exceptions to this policy are documented as they are made.

## Problem

Without an explicit policy, new analyst features risked being built for the Swing UI even as the project's 5.0 direction shifts the primary analyst interface to the browser UI, duplicating effort across both UIs.

## Acceptance criteria

- [x] Policy documented: Swing analysis UI is in maintenance mode; new features go to browser UI first.
- [x] Exceptions to the policy are logged in `specs/96-swing-parity-gap.md` §F.

## Updates

### 2026-06-21
- Issue created during roadmap consolidation, extracted from `iped-app-ROADMAP.md`, status set to `done` based on the original `[x]` marker.
