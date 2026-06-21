# ISSUE-208: Inventory and prioritize remaining iped-parsers-impl split candidates

- Status: in_progress
- Roadmap: [iped-parsers-ROADMAP.md](../roadmaps/iped-parsers-ROADMAP.md)
- Roadmap section: Phase 1 — Finish the per-parser split
- Owner: unassigned
- Created: 2026-06-21
- Updated: 2026-06-21

## Summary

`iped-parsers-impl` still aggregates a long tail of parsers not yet split into their
own Maven modules. An inventory pass identified 240 remaining classes and ranked split
candidates by priority (highest churn / most self-contained): `iped-parser-whatsapp`
(60+ classes, bencode + sqlite deps), `iped-parser-telegram` (10+ classes,
telegram-decoder-api dep), `iped-parser-threema` (10+ classes), `iped-parser-ufed`
(30+ classes for UFED XML extraction), and `iped-parser-compression`
(SevenZipParser, RARParser, LZFSEParser, PackageParser).

## Problem

Without a prioritized split plan, further modularization work has no clear order of
attack, and parsers continue to accumulate in the impl aggregate.

## Acceptance criteria

- [x] Inventory the remaining classes in `iped-parsers-impl`.
- [x] Rank split candidates by churn / self-containedness.
- [ ] All listed candidate modules actually split out (tracked individually in
      ISSUE-209 through ISSUE-213).

## Updates

### 2026-06-21
- Issue created during roadmap consolidation, extracted from `iped-parsers-ROADMAP.md`, status set to `in_progress` based on the original `[~]` marker.
