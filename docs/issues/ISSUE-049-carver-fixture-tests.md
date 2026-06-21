# ISSUE-049: Fixture tests for SQLite, DER, and EML carvers

- Status: done
- Roadmap: [iped-carvers-ROADMAP.md](../roadmaps/iped-carvers-ROADMAP.md)
- Roadmap section: Phase 1 — Hygiene
- Owner: unassigned
- Created: 2026-06-21
- Updated: 2026-06-21

## Summary

Add fixture tests covering `CarverType` metadata, signature decode lengths, header/footer counts, and size-bound assertions for the SQLite, DER, and EML carvers, increasing confidence in the new TOML-driven configuration.

## Problem

The carver configuration migration to TOML needed concrete test coverage to verify carver metadata and signature behavior matches the legacy XML-driven configuration.

## Acceptance criteria

- [x] `SQLiteCarverFixtureTest` added.
- [x] `DERCarverFixtureTest` added.
- [x] `EMLCarverFixtureTest` added.
- [x] Tests cover CarverType metadata, signature decode lengths, header/footer counts, and size-bound assertions.

## Updates

### 2026-06-21
- Issue created during roadmap consolidation, extracted from `iped-carvers-ROADMAP.md`, status set to `done` based on the original `[x]` marker.
