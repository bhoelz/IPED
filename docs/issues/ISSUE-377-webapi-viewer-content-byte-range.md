# ISSUE-377: Viewer content endpoints with byte-range support

- Status: done
- Roadmap: [iped-webapi-ROADMAP.md](../roadmaps/iped-webapi-ROADMAP.md)
- Roadmap section: Phase 3 — Jobs and streaming
- Owner: unassigned
- Created: 2026-06-21
- Updated: 2026-06-21

## Summary

Add content and text endpoints supporting HTTP byte-range requests and text highlighting/snippets, enabling efficient paged binary navigation for the hex viewer and text preview.

## Problem

Viewer islands (hex viewer, text preview) need to fetch partial content efficiently and highlight search terms, which the API did not yet support.

## Acceptance criteria

- [x] `ContentV2.java` — `GET /v2/sources/{src}/items/{id}/content` supports HTTP Range requests (RFC 7233 single-range: `bytes=N-M`, `bytes=N-`, `bytes=-N`); returns 206 Partial Content with `Content-Range` and `Accept-Ranges: bytes` headers.
- [x] `TextV2.java` — `GET /v2/sources/{src}/items/{id}/text` supports `?highlight=term1+term2` (wraps matches in `<mark>` when `Accept: text/html`) and `?limit=N` for preview snippets; returns 404 when source/item not found.

## Updates

### 2026-06-21
- Issue created during roadmap consolidation, extracted from `iped-webapi-ROADMAP.md`, status set to `done` based on the original `[x]` marker.
