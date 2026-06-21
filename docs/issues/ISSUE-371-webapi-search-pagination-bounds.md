# ISSUE-371: Pagination/cursor semantics bounded for large result sets

- Status: done
- Roadmap: [iped-webapi-ROADMAP.md](../roadmaps/iped-webapi-ROADMAP.md)
- Roadmap section: Phase 1 — v2 contract completion
- Owner: unassigned
- Created: 2026-06-21
- Updated: 2026-06-21

## Summary

Enforce sane bounds on search pagination parameters so large cases cannot produce unbounded responses, per the root roadmap's large-case pagination NFR.

## Problem

Without enforced bounds, search requests against large cases could request unbounded result pages, risking memory/performance issues.

## Acceptance criteria

- [x] `GET /v2/search` enforces `offset ≥ 0` and `1 ≤ limit ≤ 1000`, with a default limit of 50.
- [x] `SearchPage` carries the total count so the UI can compute page counts.

## Updates

### 2026-06-21
- Issue created during roadmap consolidation, extracted from `iped-webapi-ROADMAP.md`, status set to `done` based on the original `[x]` marker.
