# ISSUE-379: v1 versioning/deprecation policy and headers

- Status: done
- Roadmap: [iped-webapi-ROADMAP.md](../roadmaps/iped-webapi-ROADMAP.md)
- Roadmap section: Phase 4 — Platform (5.0)
- Owner: unassigned
- Created: 2026-06-21
- Updated: 2026-06-21

## Summary

Establish and enforce a v1 freeze/deprecation schedule now that v2 has reached parity, signaling deprecation to clients via standard HTTP headers.

## Problem

With v2 endpoints reaching parity, v1 needed a formal deprecation signal so clients migrate off it on a predictable schedule.

## Acceptance criteria

- [x] `V1DeprecationFilter.java` — Jersey `ContainerResponseFilter` at `HEADER_DECORATOR` priority.
- [x] All v1 paths (anything not under `/v2/`, `/swagger`, `/openapi`, `/webjars`) receive `Deprecation: true`, `Sunset: Sun, 01 Jan 2027 00:00:00 GMT`, and `Link: </v2/cases>; rel="successor-version"` headers.

## Updates

### 2026-06-21
- Issue created during roadmap consolidation, extracted from `iped-webapi-ROADMAP.md`, status set to `done` based on the original `[x]` marker.
