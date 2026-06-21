# ISSUE-082: Move remaining datasource-specific code to sibling modules

- Status: in_progress
- Roadmap: [iped-engine-ROADMAP.md](../roadmaps/iped-engine-ROADMAP.md)
- Roadmap section: Phase 1 — Slim down to orchestration
- Owner: unassigned
- Created: 2026-06-21
- Updated: 2026-06-21

## Summary

Move remaining datasource-specific code out of `iped-engine` and into `iped-sleuthkit`/`iped-ufed`/`iped-ad1`, wiring the seam through `iped-api` SPIs.

## Problem

`iped-engine` still contains datasource leftovers that belong in the dedicated datasource modules. The SPI contract (`iped.datasource.spi.IDataSourceReader` in iped-api with `ServiceLoader` registration) has been defined, but the physical code move has not happened yet — it requires coordinated changes to `iped-sleuthkit`/`iped-ufed`/`iped-ad1` modules outside this branch's scope.

## Acceptance criteria

- [x] Define `IDataSourceReader` SPI contract in `iped-api` with `ServiceLoader` registration.
- [ ] Move Sleuthkit-specific code from `iped-engine` to `iped-sleuthkit`.
- [ ] Move UFED-specific code from `iped-engine` to `iped-ufed`.
- [ ] Move AD1-specific code from `iped-engine` to `iped-ad1`.

## Updates

### 2026-06-21
- Issue created during roadmap consolidation, extracted from `iped-engine-ROADMAP.md`, status set to `in_progress`.
