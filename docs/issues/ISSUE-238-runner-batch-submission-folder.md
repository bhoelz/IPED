# ISSUE-238: Batch submission for a folder of evidence

- Status: done
- Roadmap: [iped-runner-ROADMAP.md](../roadmaps/iped-runner-ROADMAP.md)
- Roadmap section: Phase 2 — Scheduling and queueing
- Owner: unassigned
- Created: 2026-06-21
- Updated: 2026-06-21

## Summary

`POST /runs/batch` accepts a source directory and generates one run per immediate child, letting an operator point at a folder of evidence instead of submitting runs one at a time.

## Problem

Submitting many similar runs one-by-one was tedious for batches of evidence items sharing the same output directory and profile.

## Acceptance criteria

- [x] `POST /runs/batch` accepts `{sourceDir, outputDir, priority?, profile?, extraTokens?[]}`.
- [x] One run is enqueued per immediate child of `sourceDir`.
- [x] The endpoint returns the list of enqueued run IDs.

## Updates

### 2026-06-21
- Issue created during roadmap consolidation, extracted from `iped-runner-ROADMAP.md`, status set to `done` based on the original `[x]` marker.
