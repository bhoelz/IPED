# ISSUE-244: Contract alignment with iped-webapi job endpoints

- Status: done
- Roadmap: [iped-runner-ROADMAP.md](../roadmaps/iped-runner-ROADMAP.md)
- Roadmap section: Phase 4 — 5.0 integration
- Owner: unassigned
- Created: 2026-06-21
- Updated: 2026-06-21

## Summary

`iped-runner` and `iped-webapi` now emit identical JSON job shapes, so the browser UI and MCP tools can treat either backend as a source of truth for job status.

## Problem

Two separate job models (runner vs webapi) would force consumers to special-case which backend they were talking to.

## Acceptance criteria

- [x] Both modules emit the same JSON job shape for job status responses.

## Updates

### 2026-06-21
- Issue created during roadmap consolidation, extracted from `iped-runner-ROADMAP.md`, status set to `done` based on the original `[x]` marker.
