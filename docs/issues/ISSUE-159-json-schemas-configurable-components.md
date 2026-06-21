# ISSUE-159: JSON schemas for configurable components (31 files)

- Status: done
- Roadmap: [iped-engine-schemas-ROADMAP.md](../roadmaps/iped-engine-schemas-ROADMAP.md)
- Roadmap section: Current Status — Completed
- Owner: unassigned
- Created: 2026-06-21
- Updated: 2026-06-21

## Summary

Author JSON Schema (Draft-compatible) definitions for all 31 configurable IPED components, covering structure and types for each component's configuration shape.

## Problem

IPED's configurable components had no formal, machine-checkable schema describing their valid configuration shapes, making it hard to validate configs or build tooling (UI, CLI help, IDE support) on top of them.

## Acceptance criteria

- [x] 31 JSON schemas authored for configurable components, one per component getConfiguration() shape.

## Updates

### 2026-06-21
- Issue created during roadmap consolidation, extracted from `iped-engine-schemas-ROADMAP.md` ("Current Status — Completed"). This is one of several grouped deliverables from a ~93-checkbox historical completed-work log; grouped per the sizing rule rather than filed as 31 individual issues. Status set to `done`.
