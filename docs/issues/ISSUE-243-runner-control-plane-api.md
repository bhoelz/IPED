# ISSUE-243: Become the control-plane API of the 5.0 target architecture

- Status: done
- Roadmap: [iped-runner-ROADMAP.md](../roadmaps/iped-runner-ROADMAP.md)
- Roadmap section: Phase 4 — 5.0 integration
- Owner: unassigned
- Created: 2026-06-21
- Updated: 2026-06-21

## Summary

`RunJobsV2Controller` exposes `GET /v2/jobs`, `GET /v2/jobs/{id}`, and `DELETE /v2/jobs/{id}` in a webapi-compatible JSON shape, positioning the runner as the control-plane API consumed by the browser UI and MCP `job_*` tools rather than maintaining its own parallel UI surface long-term.

## Problem

The root roadmap's target architecture calls for a single case/job orchestration surface; the runner needed a v2 jobs API aligned with that model instead of a bespoke one.

## Acceptance criteria

- [x] `RunJobsV2Controller` provides `GET /v2/jobs`, `GET /v2/jobs/{id}`, `DELETE /v2/jobs/{id}`.
- [x] Response shape matches `{id, type: "iped-run", status, progress, message, createdAt, params}`.
- [x] Status strings match `iped-webapi` JobsV2 (`pending/running/completed/failed/cancelled`).
- [x] Active runs, queued runs, and history entries are de-duplicated by id in the unified view.

## Updates

### 2026-06-21
- Issue created during roadmap consolidation, extracted from `iped-runner-ROADMAP.md`, status set to `done` based on the original `[x]` marker.
