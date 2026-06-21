# ISSUE-364: Keep Swing viewer implementations functional until parity validated

- Status: planned
- Roadmap: [iped-viewers-ROADMAP.md](../roadmaps/iped-viewers-ROADMAP.md)
- Roadmap section: Phase 3 — Companion app bridge (5.0 workstream 2)
- Owner: unassigned
- Created: 2026-06-21
- Updated: 2026-06-21

## Summary

Keep Swing viewer implementations functional until browser/companion parity is validated per viewer, using parity tests run against a golden item set.

## Problem

Removing or letting Swing viewers degrade before their browser/companion replacements are proven equivalent would regress functionality for users still on the desktop client during the transition period.

## Acceptance criteria

- [ ] Parity tests defined against a golden item set, per viewer.
- [ ] Swing implementations kept functional until their replacement passes parity validation.

## Updates

### 2026-06-21
- Issue created during roadmap consolidation, extracted from `iped-viewers-ROADMAP.md`, status set to `planned` based on the original `[ ]` marker.
