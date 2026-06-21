# ISSUE-336: Align config UI with TOML deviation-layer model

- Status: planned
- Roadmap: [iped-ui-ROADMAP.md](../roadmaps/iped-ui-ROADMAP.md)
- Roadmap section: Phase 1 — If kept (interim hardening)
- Owner: unassigned
- Created: 2026-06-21
- Updated: 2026-06-21

## Summary

If `iped-ui` is kept, align it with the TOML configuration model by rendering module-owned defaults as a read-only baseline and only allowing edits to the deviation layer (locals/profiles), never flattening the layering into a single file.

## Problem

The current config UI risks flattening the layered TOML config model (module defaults + locals/profiles) into a single file on save, which would break the deviation-only editing convention adopted elsewhere in the project.

## Acceptance criteria

- [ ] Module-owned config defaults rendered as read-only baseline in the UI.
- [ ] Edits apply only to the deviation layer (locals/profiles).
- [ ] Saving never flattens layered config into one file.

## Updates

### 2026-06-21
- Issue created during roadmap consolidation, extracted from `iped-ui-ROADMAP.md`, status set to `planned` based on the original `[ ]` marker.
