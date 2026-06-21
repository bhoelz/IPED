# ISSUE-296: Verify per-case TSK isolation under multi-case JNI load

- Status: planned
- Roadmap: [iped-sleuthkit-ROADMAP.md](../roadmaps/iped-sleuthkit-ROADMAP.md)
- Roadmap section: Phase 2 — Robustness
- Owner: unassigned
- Created: 2026-06-21
- Updated: 2026-06-21

## Summary

Because JNI handles are process-global, memory behavior under multi-case load needs verification to ensure TSK case databases remain properly isolated per case rather than leaking state across cases.

## Problem

It's not yet verified that running multiple cases concurrently against the same process-global JNI bindings keeps each case's TSK database properly isolated.

## Acceptance criteria

- [ ] A test exercises multiple concurrent cases against the Sleuthkit reader in one process.
- [ ] TSK case databases are verified isolated per case with no cross-case state leakage.

## Updates

### 2026-06-21
- Issue created during roadmap consolidation, extracted from `iped-sleuthkit-ROADMAP.md`, status set to `planned` based on the original `[ ]` marker.
