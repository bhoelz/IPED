# ISSUE-393: Angular Elements islands build target

- Status: done
- Roadmap: [iped-webui-ROADMAP.md](../roadmaps/iped-webui-ROADMAP.md)
- Roadmap section: Current state
- Owner: unassigned
- Created: 2026-06-21
- Updated: 2026-06-21

## Summary

An Angular Elements `islands` build target was established — zoneless, signals-based, producing a hashed bundle suitable for embedding in the SSR composition root.

## Problem

The webui workspace needed a build target that produces standalone custom-element bundles rather than a single SPA bundle.

## Acceptance criteria

- [x] `islands` build target produces a zoneless, signals-based, hashed bundle.

## Updates

### 2026-06-21
- Issue created during roadmap consolidation, extracted from `iped-webui-ROADMAP.md`, status set to `done` based on the original `[x]` marker.
