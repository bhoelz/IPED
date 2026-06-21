# ISSUE-407: Accessibility pass — keyboard navigation in grid/viewer islands

- Status: done
- Roadmap: [iped-webui-ROADMAP.md](../roadmaps/iped-webui-ROADMAP.md)
- Roadmap section: Phase 4 — Quality bar
- Owner: unassigned
- Created: 2026-06-21
- Updated: 2026-06-21

## Summary

`results-grid` rows gained `role="row"`, `tabindex="0"`, `aria-selected`, `aria-label`, and Enter/Space key bindings; `gallery` cells gained `role="gridcell"` with the same pattern; thumbnail `alt` text changed from empty to a descriptive label.

## Problem

The grid and gallery islands were not keyboard-navigable or screen-reader friendly, a gap for accessibility compliance.

## Acceptance criteria

- [x] Results-grid rows are keyboard-navigable with proper ARIA roles/labels.
- [x] Gallery cells are keyboard-navigable with proper ARIA roles/labels.
- [x] Thumbnail `alt` text is descriptive, not empty.

## Updates

### 2026-06-21
- Issue created during roadmap consolidation, extracted from `iped-webui-ROADMAP.md`, status set to `done` based on the original `[x]` marker.
