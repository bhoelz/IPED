# ISSUE-051: Eliminate redundant buffer copy in the carving scan path

- Status: done
- Roadmap: [iped-carvers-ROADMAP.md](../roadmaps/iped-carvers-ROADMAP.md)
- Roadmap section: Phase 2 — Performance
- Owner: unassigned
- Created: 2026-06-21
- Updated: 2026-06-21

## Summary

Eliminate the redundant buffer copy in `CarverTask.fillBuf()`, which previously copied each 1 MB chunk into a fresh `cBuf` array before passing it to `AhoCorasick.continueSearch()`. This removes one `new byte[len]` + `System.arraycopy` per 1 MB chunk scanned.

## Problem

`CarverTask.fillBuf()` allocated and copied a fresh buffer for every 1 MB chunk scanned, adding unnecessary allocation and copy overhead to the hot carving scan path.

## Acceptance criteria

- [x] `SearchResult` carries a `length` field (new overload constructor) so scanning stops at `length` rather than `bytes.length`.
- [x] `AhoCorasick.continueSearch()` uses `lastResult.length` in both search paths.
- [x] `CarverTask` passes the shared `buf` directly with the valid `len`, eliminating the per-chunk allocation and copy.

## Updates

### 2026-06-21
- Issue created during roadmap consolidation, extracted from `iped-carvers-ROADMAP.md`, status set to `done` based on the original `[x]` marker.
