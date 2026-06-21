# ISSUE-321: Image task — embedding generation hook feeding the vector store connector

- Status: planned
- Roadmap: [iped-tasks-ROADMAP.md](../roadmaps/iped-tasks-ROADMAP.md)
- Roadmap section: Phase 4 — Feature growth (5.0)
- Owner: unassigned
- Created: 2026-06-21
- Updated: 2026-06-21

## Summary

The image task should gain an embedding generation hook that feeds the vector
store connector (`iped-additional-index`), as part of the 5.0 feature growth
workstream.

## Problem

There is currently no hook in the image processing task that generates embeddings
for downstream consumption by the vector store connector, limiting similarity
search and other vector-based features for image content.

## Acceptance criteria

- [ ] Add an embedding generation hook to the image task pipeline.
- [ ] Feed generated embeddings into the `iped-additional-index` vector store
      connector.

## Updates

### 2026-06-21
- Issue created during roadmap consolidation, extracted from `iped-tasks-ROADMAP.md`, status set to `planned` based on the original `[ ]` marker.
