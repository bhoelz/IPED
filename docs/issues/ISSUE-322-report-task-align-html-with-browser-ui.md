# ISSUE-322: Report task — align HTML report output with the browser-UI rendering stack

- Status: planned
- Roadmap: [iped-tasks-ROADMAP.md](../roadmaps/iped-tasks-ROADMAP.md)
- Roadmap section: Phase 4 — Feature growth (5.0)
- Owner: unassigned
- Created: 2026-06-21
- Updated: 2026-06-21

## Summary

The report task's HTML output should be aligned with the new browser-UI rendering
stack instead of the legacy static templates, as part of the 5.0 feature growth
workstream. This is related to the parser-side report decoupling work tracked in
`iped-parsers-ROADMAP.md` Phase 3 (ISSUE-223).

## Problem

The report task currently produces HTML using legacy static templates that assume
the old Swing-based viewer, which doesn't align with the browser-based UI direction
for 5.0.

## Acceptance criteria

- [ ] Identify legacy static-template assumptions in the report task's HTML
      generation.
- [ ] Align report HTML output with the browser-UI rendering stack.

## Updates

### 2026-06-21
- Issue created during roadmap consolidation, extracted from `iped-tasks-ROADMAP.md`, status set to `planned` based on the original `[ ]` marker.
