# ISSUE-082: Move remaining datasource-specific code to sibling modules

- Status: done
- Roadmap: [iped-engine-ROADMAP.md](../roadmaps/iped-engine-ROADMAP.md)
- Roadmap section: Phase 1 — Slim down to orchestration
- Owner: unassigned
- Created: 2026-06-21
- Updated: 2026-07-17

## Summary

Move remaining datasource-specific code out of `iped-engine` and into `iped-sleuthkit`/`iped-ufed`/`iped-ad1`, wiring the seam through `iped-api` SPIs.

## Problem

`iped-engine` still contains datasource leftovers that belong in the dedicated datasource modules. The SPI contract (`iped.datasource.spi.IDataSourceReader` in iped-api with `ServiceLoader` registration) has been defined, but the physical code move has not happened yet — it requires coordinated changes to `iped-sleuthkit`/`iped-ufed`/`iped-ad1` modules outside this branch's scope.

## Acceptance criteria

- [x] Define `IDataSourceReader` SPI contract in `iped-api` with `ServiceLoader` registration.
- [x] Move Sleuthkit-specific code from `iped-engine` to `iped-sleuthkit`.
- [x] Move UFED-specific code from `iped-engine` to `iped-ufed`.
- [x] Move AD1-specific code from `iped-engine` to `iped-ad1`.

## Updates

### 2026-06-21
- Issue created during roadmap consolidation, extracted from `iped-engine-ROADMAP.md`, status set to `in_progress`.

### 2026-07-17
- Verified that `SleuthkitReader`, `UfedXmlReader` and `AD1DataSourceReader` are owned by
  `iped-sleuthkit`, `iped-ufed` and `iped-ad1`, respectively; no implementation copy remains in
  `iped-engine`.
- The engine retains only generic orchestration (`ItemProducer`, `IPEDReader` and
  `FolderTreeReader`); datasource-specific implementations are supplied by sibling modules.
- The existing `iped-api` `IDataSourceReader` SPI remains the integration seam for the next
  ServiceLoader wiring issue (ISSUE-132).
