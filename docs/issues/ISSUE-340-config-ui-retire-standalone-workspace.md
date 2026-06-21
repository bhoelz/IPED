# ISSUE-340: Retire the standalone iped-ui workspace

- Status: planned
- Roadmap: [iped-ui-ROADMAP.md](../roadmaps/iped-ui-ROADMAP.md)
- Roadmap section: Phase 2 — Migration (if decision = fold into web UI)
- Owner: unassigned
- Created: 2026-06-21
- Updated: 2026-06-21

## Summary

Once configuration screens are ported into `iped-webui-server`, retire the standalone `iped-ui` npm workspace and record the removal in `MIGRATION_GUIDE.md`.

## Problem

Keeping the standalone workspace around after migration would leave a second, stale source of configuration-editing code in the repo.

## Acceptance criteria

- [ ] Standalone `iped-ui` workspace removed from the repo.
- [ ] Removal recorded in `MIGRATION_GUIDE.md`.

## Updates

### 2026-06-21
- Issue created during roadmap consolidation, extracted from `iped-ui-ROADMAP.md`, status set to `planned` based on the original `[ ]` marker.
