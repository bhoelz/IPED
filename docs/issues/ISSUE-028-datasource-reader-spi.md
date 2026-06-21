# ISSUE-028: Capability/SPI interfaces for pluggable datasource readers

- Status: done
- Roadmap: [iped-api-ROADMAP.md](../roadmaps/iped-api-ROADMAP.md)
- Roadmap section: Phase 3 — Modularization support
- Owner: unassigned
- Created: 2026-06-21
- Updated: 2026-06-21

## Summary

Add capability/SPI interfaces for pluggable datasource readers so that `iped-sleuthkit`, `iped-ufed`, and `iped-ad1` can register via `ServiceLoader` instead of relying on hardcoded engine wiring, supporting the planned module split.

## Problem

Datasource readers are currently wired directly into the engine, which blocks splitting `iped-sleuthkit`/`iped-ufed`/`iped-ad1` into independent, pluggable modules.

## Acceptance criteria

- [x] `iped.datasource.spi.IDataSourceReader` added.

## Updates

### 2026-06-21
- Issue created during roadmap consolidation, extracted from `iped-api-ROADMAP.md`, status set to `done` based on the original `[x]` marker.
