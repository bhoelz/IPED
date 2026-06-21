# ISSUE-160: UI schemas for configurable components (31 files)

- Status: done
- Roadmap: [iped-engine-schemas-ROADMAP.md](../roadmaps/iped-engine-schemas-ROADMAP.md)
- Roadmap section: Current Status — Completed
- Owner: unassigned
- Created: 2026-06-21
- Updated: 2026-06-21

## Summary

Author UI schema definitions for all 31 configurable IPED components, complementing the JSON schemas with widget/layout hints suitable for rendering configuration forms.

## Problem

The JSON schemas alone describe valid data shapes but not how a configuration form should be rendered (widget choice, grouping, ordering); a parallel UI schema set was needed for any future configuration UI to consume.

## Acceptance criteria

- [x] 31 UI schemas authored, one per configurable component, paired with the corresponding JSON schema.

## Updates

### 2026-06-21
- Issue created during roadmap consolidation, extracted from `iped-engine-schemas-ROADMAP.md` ("Current Status — Completed"). This is one of several grouped deliverables from a ~93-checkbox historical completed-work log; grouped per the sizing rule rather than filed as 31 individual issues. Status set to `done`.
