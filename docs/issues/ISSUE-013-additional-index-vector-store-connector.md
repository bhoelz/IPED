# ISSUE-013: Vector store connector for semantic/similarity search

- Status: done
- Roadmap: [iped-additional-index-ROADMAP.md](../roadmaps/iped-additional-index-ROADMAP.md)
- Roadmap section: Phase 2 — First-class connectors
- Owner: unassigned
- Created: 2026-06-21
- Updated: 2026-07-16

## Summary

A vector store connector is needed to support semantic/similarity search, including embedding pipeline hooks implemented as a processing task with storage handled behind the connector contract defined in Phase 1.

## Problem

IPED currently has no first-class connector for vector/embedding-based similarity search across evidence items.

## Acceptance criteria

- [ ] Embedding pipeline hooks exist as a processing task.
- [ ] Vector storage is implemented behind the connector SPI (not hardcoded to a specific store).
- [ ] Similarity search is queryable against the connector.

## Updates

### 2026-06-21
- Issue created during roadmap consolidation, extracted from `iped-additional-index-ROADMAP.md`, status set to `planned` based on the original `[ ]` marker.

### 2026-07-16
- Added `IVectorStoreConnector`, `EmbeddingPipelineHook`, and a reference in-memory connector with cosine similarity and item-ID traceability tests.
