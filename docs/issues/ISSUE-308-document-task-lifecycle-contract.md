# ISSUE-308: Document the task lifecycle contract in TaskProvider Javadoc

- Status: done
- Roadmap: [iped-tasks-ROADMAP.md](../roadmaps/iped-tasks-ROADMAP.md)
- Roadmap section: Phase 2 — SPI maturity
- Owner: unassigned
- Created: 2026-06-21
- Updated: 2026-06-21

## Summary

The task lifecycle contract (`init`/`process`/`finish`, per-worker instances,
thread-safety expectations) is now documented as a complete contract reference in
`TaskProvider` Javadoc, for third-party implementors of the task SPI.

## Problem

Third-party task implementors had no authoritative reference describing the
expected lifecycle calls, instancing model, or thread-safety guarantees of the
`TaskProvider` SPI.

## Acceptance criteria

- [x] `init`/`process`/`finish` lifecycle documented in `TaskProvider` Javadoc.
- [x] Per-worker instancing behavior documented.
- [x] Thread-safety expectations documented.

## Updates

### 2026-06-21
- Issue created during roadmap consolidation, extracted from `iped-tasks-ROADMAP.md`, status set to `done` based on the original `[x]` marker.
