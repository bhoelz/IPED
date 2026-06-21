# ISSUE-381: Jersey-vs-Spring stack consolidation decision

- Status: done
- Roadmap: [iped-webapi-ROADMAP.md](../roadmaps/iped-webapi-ROADMAP.md)
- Roadmap section: Phase 4 — Platform (5.0)
- Owner: unassigned
- Created: 2026-06-21
- Updated: 2026-06-21

## Summary

Decide, before broad v2 expansion, whether to keep Jersey as the engine-API (with `iped-webui-server` proxying to it) or fold endpoints into a single Spring stack.

## Problem

Two web stacks existed in the architecture (Jersey for the engine API, Spring Boot for `iped-webui-server`), and broad v2 expansion needed a clear decision on whether to consolidate before investing further.

## Acceptance criteria

- [x] Decision made and recorded: keep Jersey on embedded Jetty for the engine JVM; `iped-webui-server` (Spring Boot) proxies via `RestTemplate`.
- [x] Decision gate closed before broad v2 expansion proceeded.

## Updates

### 2026-06-21
- Issue created during roadmap consolidation, extracted from `iped-webapi-ROADMAP.md`, status set to `done` based on the original `[x]` marker.
