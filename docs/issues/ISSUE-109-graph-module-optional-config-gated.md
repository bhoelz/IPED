# ISSUE-109: Make iped-engine-graph fully optional via config gating

- Status: planned
- Roadmap: [iped-engine-graph-ROADMAP.md](../roadmaps/iped-engine-graph-ROADMAP.md)
- Roadmap section: Phase 1 — Module boundary
- Owner: unassigned
- Created: 2026-06-21
- Updated: 2026-06-21

## Summary

A distribution that doesn't include the graph module must still process cases normally with graph features disabled, gated by configuration and reporting a clear log message rather than failing.

## Problem

It's not yet verified that the engine degrades gracefully — with a clear log message — when the graph module is absent or disabled via configuration.

## Acceptance criteria

- [ ] Case processing succeeds with the graph module absent from the classpath.
- [ ] Graph features are config-gated and produce a clear log message when disabled.

## Updates

### 2026-06-21
- Issue created during roadmap consolidation, extracted from `iped-engine-graph-ROADMAP.md`, status set to `planned` based on the original `[ ]` marker.
