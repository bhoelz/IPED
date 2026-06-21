# ISSUE-330: Unknown-element telemetry for UFED artifact mapping

- Status: planned
- Roadmap: [iped-ufed-ROADMAP.md](../roadmaps/iped-ufed-ROADMAP.md)
- Roadmap section: Phase 2 — Format coverage
- Owner: unassigned
- Created: 2026-06-21
- Updated: 2026-06-21

## Summary

The mapper should log and count artifact types it doesn't recognize or handle, so coverage gaps in UFED artifact mapping become visible via telemetry instead of failing silently.

## Problem

Artifact types unhandled by the mapper currently go unnoticed, hiding format coverage gaps from maintainers.

## Acceptance criteria

- [ ] Unmapped/unknown UFED element types are logged.
- [ ] A count of unknown element types encountered is available (e.g. in processing reports or logs).

## Updates

### 2026-06-21
- Issue created during roadmap consolidation, extracted from `iped-ufed-ROADMAP.md`, status set to `planned` based on the original `[ ]` marker.
