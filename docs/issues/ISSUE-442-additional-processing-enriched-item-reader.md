# ISSUE-442: Transparent item enrichment from additional data sources

- Status: done
- Roadmap: [iped-additional-index-ROADMAP.md](../roadmaps/iped-additional-index-ROADMAP.md)
- Roadmap section: Phase 0 — Item-level additional task processing
- Owner: unassigned
- Created: 2026-06-21
- Updated: 2026-06-21

## Summary

Every item read back from a case is transparently merged with any additional-data-source results for that item, so the GUI and all viewers see enriched metadata without per-call-site changes.

## Problem

Without a single enrichment point, every consumer of `IPEDSource.getItemByID()`/`getItemByLuceneID()` would need its own logic to merge in additional-processing results.

## Acceptance criteria

- [x] `EnrichedItem` (decorator over the base item reader) implemented in `iped-additional-index`.
- [x] Wired into item retrieval so enrichment is transparent to callers.

## Updates

### 2026-06-21
- Issue created while triaging `docs/needs-revision/ADDITIONAL_PROCESSING.md` — verified `iped-engine-parent/iped-additional-index/src/main/java/iped/engine/additionalindex/EnrichedItem.java` already exists, status set to `done`.
