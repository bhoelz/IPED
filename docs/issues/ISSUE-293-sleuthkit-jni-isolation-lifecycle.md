# ISSUE-293: Isolate JNI/native-lib loading and lifecycle in iped-sleuthkit

- Status: planned
- Roadmap: [iped-sleuthkit-ROADMAP.md](../roadmaps/iped-sleuthkit-ROADMAP.md)
- Roadmap section: Phase 1 — Boundary and registration
- Owner: unassigned
- Created: 2026-06-21
- Updated: 2026-06-21

## Summary

JNI/native-library loading and its lifecycle should be fully isolated inside `iped-sleuthkit`, so the engine never needs to know TSK exists. A native load failure should surface simply as "Sleuthkit datasource unavailable" rather than an engine-level crash.

## Problem

Native library loading concerns may currently leak into the engine, and a TSK native load failure likely doesn't degrade cleanly today.

## Acceptance criteria

- [ ] All JNI/native-lib loading and lifecycle logic lives inside `iped-sleuthkit`.
- [ ] A native load failure surfaces as "Sleuthkit datasource unavailable" without crashing the engine.

## Updates

### 2026-06-21
- Issue created during roadmap consolidation, extracted from `iped-sleuthkit-ROADMAP.md`, status set to `planned` based on the original `[ ]` marker.
