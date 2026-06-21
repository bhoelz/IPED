# ISSUE-224: Evaluate sandboxing/timeout hardening for external-process parsers

- Status: planned
- Roadmap: [iped-parsers-ROADMAP.md](../roadmaps/iped-parsers-ROADMAP.md)
- Roadmap section: Phase 3 — Architecture
- Owner: unassigned
- Created: 2026-06-21
- Updated: 2026-06-21

## Summary

The `iped-parser-external` pattern (parsers that shell out to native processes)
should be evaluated for sandboxing and timeout hardening to better isolate
crash-prone native parsers from the main process.

## Problem

Crash-prone native parsers invoked via the external-process pattern currently lack
hardened isolation, risking instability in the main processing pipeline.

## Acceptance criteria

- [ ] Evaluate sandboxing options for `iped-parser-external`-style parsers.
- [ ] Evaluate/implement timeout hardening for crash-prone native parsers.

## Updates

### 2026-06-21
- Issue created during roadmap consolidation, extracted from `iped-parsers-ROADMAP.md`, status set to `planned` based on the original `[ ]` marker.
