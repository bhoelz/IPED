# ISSUE-053: Carver plugin SPI parity with parsers

- Status: done
- Roadmap: [iped-carvers-ROADMAP.md](../roadmaps/iped-carvers-ROADMAP.md)
- Roadmap section: Phase 3 — Capability growth
- Owner: unassigned
- Created: 2026-06-21
- Updated: 2026-06-21

## Summary

Add a `CarverPlugin` interface to `iped-carvers-api`, discovered via `ServiceLoader`, so third-party carver jars can be dropped onto the plugins classpath and merged into the active carver type table with zero edits to `CarverConfig.toml`/`.xml` — mirroring how Tika `Parser` plugins are discovered.

## Problem

Carvers had no plugin discovery mechanism analogous to the existing Tika `Parser` plugin SPI, making it harder for third parties to extend carving capability without modifying core configuration files.

## Acceptance criteria

- [x] `CarverPlugin` interface added to `iped-carvers-api`.
- [x] Discovered via `ServiceLoader` in `XMLCarverConfiguration` (the common base of `TomlCarverConfiguration`), right before the signature tree is built in `configListener()`.
- [x] A jar with `META-INF/services/iped.carvers.api.CarverPlugin` is merged into the active carver type table with zero edits to `CarverConfig.toml`/`.xml`.
- [x] Plugin-supplied `CarverType[]` carries any already-attached `CarvedItemValidator`s.

## Updates

### 2026-06-21
- Issue created during roadmap consolidation, extracted from `iped-carvers-ROADMAP.md`, status set to `done` based on the original `[x]` marker.
