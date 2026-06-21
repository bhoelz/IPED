# ISSUE-209: Move shared parser-split prerequisites into iped-parsers-common

- Status: done
- Roadmap: [iped-parsers-ROADMAP.md](../roadmaps/iped-parsers-ROADMAP.md)
- Roadmap section: Phase 1 — Finish the per-parser split
- Owner: unassigned
- Created: 2026-06-21
- Updated: 2026-06-21

## Summary

Before individual parser families (whatsapp, threema, telegram, ufed, compression)
could be split out of `iped-parsers-impl`, several shared types needed to live in
`iped-parsers-common` so the new modules wouldn't depend back on impl.

## Problem

Parser-family modules under split needed `IParty`/`IReferencedContact`,
`PartyStringBuilder`, `ParserConstants.INDEXER_CONTENT_TYPE`, and
`PhoneParsingConfig` available without a dependency on `iped-parsers-impl`.

## Acceptance criteria

- [x] `IParty` + `IReferencedContact` interfaces added to `iped.parsers.chat` in
      `iped-parsers-common`; `Party` implements `IParty`, `ReferencedAccountable`
      implements `IReferencedContact`.
- [x] `PartyStringBuilder` migrated to use `IParty` so it no longer touches impl.
- [x] `ParserConstants.INDEXER_CONTENT_TYPE` constant extracted from `StandardParser`.
- [x] `PhoneParsingConfig` moved from impl to common (only depends on `iped-api`).

## Updates

### 2026-06-21
- Issue created during roadmap consolidation, extracted from `iped-parsers-ROADMAP.md`, status set to `done` based on the original `[x]` marker.
