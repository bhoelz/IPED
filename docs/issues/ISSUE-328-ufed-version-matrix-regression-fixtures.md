# ISSUE-328: Version matrix and regression fixtures for UFED export formats

- Status: planned
- Roadmap: [iped-ufed-ROADMAP.md](../roadmaps/iped-ufed-ROADMAP.md)
- Roadmap section: Phase 2 — Format coverage
- Owner: unassigned
- Created: 2026-06-21
- Updated: 2026-06-21

## Summary

Because the UFED XML schema drifts release to release, a version matrix of supported UFED/Physical Analyzer export versions is needed, backed by regression fixtures per version.

## Problem

There is no documented matrix of which UFED/Physical Analyzer export versions are supported, nor per-version regression fixtures to catch schema-drift breakage.

## Acceptance criteria

- [ ] A version matrix documents supported UFED/Physical Analyzer export versions.
- [ ] Each listed version has a regression fixture exercised in tests.

## Updates

### 2026-06-21
- Issue created during roadmap consolidation, extracted from `iped-ufed-ROADMAP.md`, status set to `planned` based on the original `[ ]` marker.
