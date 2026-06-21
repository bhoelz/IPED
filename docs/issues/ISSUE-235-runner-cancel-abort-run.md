# ISSUE-235: Cancel/abort a run cleanly

- Status: done
- Roadmap: [iped-runner-ROADMAP.md](../roadmaps/iped-runner-ROADMAP.md)
- Roadmap section: Phase 1 — Run lifecycle completeness
- Owner: unassigned
- Created: 2026-06-21
- Updated: 2026-06-21

## Summary

`DELETE /run/{id}` now performs a graceful abort: SIGTERM, then a configurable wait (`runner.graceful-shutdown-seconds`, default 5), then `destroyForcibly()` if the process is still alive. The result is recorded in history with `RunStatus.ABORTED`.

## Problem

Aborting a run needed to coordinate with the distributed module's graceful-drain behavior rather than killing the process abruptly.

## Acceptance criteria

- [x] `DELETE /run/{id}` sends SIGTERM, waits `runner.graceful-shutdown-seconds`, then force-kills if needed.
- [x] Aborted runs are recorded in history with `RunStatus.ABORTED`.

## Updates

### 2026-06-21
- Issue created during roadmap consolidation, extracted from `iped-runner-ROADMAP.md`, status set to `done` based on the original `[x]` marker.
