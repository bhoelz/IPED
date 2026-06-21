# ISSUE-430: Decide long-term topology with iped-webapi

- Status: done
- Roadmap: [iped-webui-server-ROADMAP.md](../roadmaps/iped-webui-server-ROADMAP.md)
- Roadmap section: Phase 4 — Production posture (5.0)
- Owner: unassigned
- Created: 2026-06-21
- Updated: 2026-06-21

## Summary

Decision recorded: keep the two-process model for 5.0, since the engine JVM co-loads Lucene and the processing pipeline and merging classpaths into Spring Boot would conflict. The future consolidation trigger (only if `iped-webapi` decouples from the engine JVM) is documented in `DEPLOYMENT-GUIDE.md`.

## Problem

The project needed an explicit, documented decision on whether `iped-webui-server` should eventually absorb `iped-webapi` into a single process, to avoid re-litigating the question repeatedly.

## Acceptance criteria

- [x] Decision documented: keep two-process model for 5.0.
- [x] Future consolidation trigger condition documented.

## Updates

### 2026-06-21
- Issue created during roadmap consolidation, extracted from `iped-webui-server-ROADMAP.md`, status set to `done` based on the original `[x]` marker.
