# ISSUE-212: Split iped-parser-telegram out of iped-parsers-impl

- Status: done
- Roadmap: [iped-parsers-ROADMAP.md](../roadmaps/iped-parsers-ROADMAP.md)
- Roadmap section: Phase 1 — Finish the per-parser split
- Owner: unassigned
- Created: 2026-06-21
- Updated: 2026-06-21

## Summary

Telegram parsing (10+ classes, telegram-decoder-api dep) was split out of
`iped-parsers-impl` into its own Maven module.

## Problem

Telegram parser code lived inside the monolithic `iped-parsers-impl` aggregate, and
relied on a deprecated `javax.xml.bind` API and cross-module `Util` calls into the
whatsapp split.

## Acceptance criteria

- [x] `iped-parser-telegram` module created and compiles clean.
- [x] 13 parser classes + resource (css/tooltip.css) + test fixtures moved.
- [x] `META-INF/services` wired.
- [x] `iped-parser-db-base` (ItemInfo), `iped-parser-vcard` (HTML_STYLE),
      `telegram-decoder-api` deps declared.
- [x] `javax.xml.bind.DatatypeConverter` replaced with `Base64.getDecoder()`.
- [x] `iped.parsers.whatsapp.Util.*` references inlined into
      `iped.parsers.telegram.Util`; report-path helpers replicated locally.

## Updates

### 2026-06-21
- Issue created during roadmap consolidation, extracted from `iped-parsers-ROADMAP.md`, status set to `done` based on the original `[x]` marker.
