# ISSUE-320: Transcript task — pluggable ASR backends behind one config surface

- Status: planned
- Roadmap: [iped-tasks-ROADMAP.md](../roadmaps/iped-tasks-ROADMAP.md)
- Roadmap section: Phase 4 — Feature growth (5.0)
- Owner: unassigned
- Created: 2026-06-21
- Updated: 2026-06-21

## Summary

The transcript task should support pluggable ASR (automatic speech recognition)
backends — local model vs. remote service — behind a single unified configuration
surface, as part of the 5.0 feature growth workstream.

## Problem

Today the transcript task likely hardcodes (or only loosely supports) a single ASR
backend, making it difficult for operators to choose between a local model and a
remote ASR service without code changes.

## Acceptance criteria

- [ ] Define a backend-agnostic ASR interface for the transcript task.
- [ ] Support at least one local-model backend and one remote-service backend.
- [ ] Expose backend selection/config through a single config surface.

## Updates

### 2026-06-21
- Issue created during roadmap consolidation, extracted from `iped-tasks-ROADMAP.md`, status set to `planned` based on the original `[ ]` marker.
