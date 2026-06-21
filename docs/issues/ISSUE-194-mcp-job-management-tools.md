# ISSUE-194: Async job management tools (export/status/cancel)

- Status: done
- Roadmap: [iped-mcp-ROADMAP.md](../roadmaps/iped-mcp-ROADMAP.md)
- Roadmap section: Phase 3 — Write and job tools
- Owner: unassigned
- Created: 2026-06-21
- Updated: 2026-06-21

## Summary

Add `iped_job_export`/`iped_job_status`/`iped_job_cancel` tools backed by the webapi's async jobs API, letting AI clients kick off and monitor long-running export/report jobs.

## Problem

AI clients had no way to trigger or track asynchronous export/report jobs through MCP.

## Acceptance criteria

- [x] `iped_job_export`/`iped_job_status`/`iped_job_cancel` implemented in `JobTools`, backed by `POST/GET/DELETE /v2/jobs`.
- [x] Tools require the `JOBS` capability.
- [x] `JobDto` DTO added.

## Updates

### 2026-06-21
- Issue created during roadmap consolidation, extracted from `iped-mcp-ROADMAP.md`, status set to `done` based on the original `[x]` marker.
