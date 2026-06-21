# ISSUE-284: Progress check — CI builds the bundle and runner serves hashed assets

- Status: done
- Roadmap: [iped-runner-ui-ROADMAP.md](../roadmaps/iped-runner-ui-ROADMAP.md)
- Roadmap section: Progress checks
- Owner: unassigned
- Created: 2026-06-21
- Updated: 2026-06-21

## Summary

Verified that CI builds the frontend bundle and that the runner serves the resulting hashed assets correctly in a packaged build.

## Problem

The CI-to-runtime path for the frontend bundle needed end-to-end verification, not just a passing build step in isolation.

## Acceptance criteria

- [x] CI builds the bundle successfully.
- [x] Runner serves the hashed assets produced by that build.

## Updates

### 2026-06-21
- Issue created during roadmap consolidation, extracted from `iped-runner-ui-ROADMAP.md`, status set to `done` based on the original `[x]` marker.
