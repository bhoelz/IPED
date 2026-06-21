# ISSUE-280: Keyboard navigation through the runs list

- Status: done
- Roadmap: [iped-runner-ui-ROADMAP.md](../roadmaps/iped-runner-ui-ROADMAP.md)
- Roadmap section: Phase 5 — Operator UX polish
- Owner: unassigned
- Created: 2026-06-21
- Updated: 2026-06-21

## Summary

`ArrowDown`/`ArrowUp` move the selection through `filteredJobs` when the runs tab is active; `Escape` clears the selection; navigation is skipped when focus is on an INPUT/SELECT/TEXTAREA.

## Problem

Operators triaging many runs needed to move between them without reaching for the mouse each time.

## Acceptance criteria

- [x] `ArrowDown`/`ArrowUp` move selection through `filteredJobs`.
- [x] `Escape` clears the selection.
- [x] Navigation is suppressed while focus is in a form control.

## Updates

### 2026-06-21
- Issue created during roadmap consolidation, extracted from `iped-runner-ui-ROADMAP.md`, status set to `done` based on the original `[x]` marker.
