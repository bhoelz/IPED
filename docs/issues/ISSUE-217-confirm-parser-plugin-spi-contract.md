# ISSUE-217: Confirm and document the parser plugin SPI contract

- Status: done
- Roadmap: [iped-parsers-ROADMAP.md](../roadmaps/iped-parsers-ROADMAP.md)
- Roadmap section: Phase 1 — Finish the per-parser split
- Owner: unassigned
- Created: 2026-06-21
- Updated: 2026-06-21

## Summary

Verified that a parser plugin contract already exists in practice via the standard
Tika SPI mechanism, so third-party parser plugins can ship the same way the in-tree
split modules do.

## Problem

It needed to be confirmed whether a formal plugin manifest format was required for
third-party parser plugins, or whether the existing Tika SPI mechanism was
sufficient.

## Acceptance criteria

- [x] Confirm every split-out module registers via the standard Tika SPI
      (`META-INF/services/org.apache.tika.parser.Parser` +
      `getSupportedTypes(ParseContext)` declaring MIME types).
- [x] Confirm Tika's own composite/priority resolution governs ordering, with no
      additional manifest format needed.
- [x] Confirm carving got an equivalent contract this round (see `iped-carvers`
      Phase 3 `CarverPlugin` SPI).

## Updates

### 2026-06-21
- Issue created during roadmap consolidation, extracted from `iped-parsers-ROADMAP.md`, status set to `done` based on the original `[x]` marker.
