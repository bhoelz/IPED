# ISSUE-383: Record Jersey-vs-Spring consolidation decision and stack boundaries

- Status: done
- Roadmap: [iped-webapi-ROADMAP.md](../roadmaps/iped-webapi-ROADMAP.md)
- Roadmap section: Phase 5 — Bookmark CRUD and stack consolidation
- Owner: unassigned
- Created: 2026-06-21
- Updated: 2026-06-21

## Summary

Formally record the rationale for keeping Jersey as the engine-side API and Spring as the SSR/auth layer, documenting why the two stacks remain cleanly separated rather than merged.

## Problem

The decision to keep two web stacks needed durable documentation of the reasoning (classpath/runtime constraints), so future contributors don't re-open the question without context.

## Acceptance criteria

- [x] Decision recorded: keep Jersey as the engine-side API because `iped-webapi` must start inside the engine JVM, co-loaded with Lucene, IItem, and the processing pipeline, where Spring Boot's classpath management would conflict.
- [x] `iped-webui-server` (Spring Boot) documented as proxying to Jersey via a thin `RestTemplate` reverse-proxy.
- [x] Stack boundaries documented: Jersey owns forensic data access; Spring owns SSR page composition, auth, and island serving.

## Updates

### 2026-06-21
- Issue created during roadmap consolidation, extracted from `iped-webapi-ROADMAP.md`, status set to `done` based on the original `[x]` marker.
