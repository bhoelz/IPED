# ISSUE-033: Extract release/distribution assembly into a dedicated packaging module

- Status: planned
- Roadmap: [iped-app-ROADMAP.md](../roadmaps/iped-app-ROADMAP.md)
- Roadmap section: Phase 1 — Separate assembly from application
- Owner: unassigned
- Created: 2026-06-21
- Updated: 2026-06-21

## Summary

Move the release/distribution assembly responsibilities (conf aggregation, lib/tools/plugins layout, launcher scripts) currently living in `iped-app` into a dedicated packaging module, so changes to the Swing UI don't churn the release pipeline and vice versa.

## Problem

`iped-app` currently has two responsibilities bundled together: it is both the desktop Swing application and the owner of the `release.dir` assembly (conf/lib/tools/plugins layout, launcher scripts). This coupling means UI-only changes can unintentionally affect the release packaging, and packaging changes require touching the UI module.

## Acceptance criteria

- [ ] A dedicated packaging module exists that owns conf aggregation, lib/tools/plugins layout, and launcher scripts.
- [ ] `iped-app` no longer needs to change when only packaging/assembly concerns change.
- [ ] Release builds continue to produce the same `release.dir` layout after the extraction.

## Updates

### 2026-06-21
- Issue created during roadmap consolidation, extracted from `iped-app-ROADMAP.md`, status set to `planned` based on the original `[ ]` marker.
