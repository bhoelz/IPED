# ISSUE-213: Split iped-parser-ufed out of iped-parsers-impl

- Status: done
- Roadmap: [iped-parsers-ROADMAP.md](../roadmaps/iped-parsers-ROADMAP.md)
- Roadmap section: Phase 1 — Finish the per-parser split
- Owner: unassigned
- Created: 2026-06-21
- Updated: 2026-06-21

## Summary

UFED XML extraction (30+ classes) was split out of `iped-parsers-impl` into its own
Maven module, including moving several party/report helper classes into
`iped-parsers-common` for reuse.

## Problem

UFED parser/handler/model/reference/util code lived inside the monolithic
`iped-parsers-impl` aggregate, used a Tika 2.x `HtmlParser` API removed in Tika 3.x,
and referenced `StandardParser.INDEXER_CONTENT_TYPE` instead of the common constant.

## Acceptance criteria

- [x] `iped-parser-ufed` module created and compiles clean.
- [x] 43 parser/handler/model/reference/util classes moved; `META-INF/services` wired
      for 4 parsers.
- [x] `HtmlParser` usage replaced with `JSoupParser` (Tika 3.x).
- [x] `StandardParser.INDEXER_CONTENT_TYPE` references switched to
      `ParserConstants.INDEXER_CONTENT_TYPE`.
- [x] `iped.parsers.util.Util.*` calls in `ReportGenerator` resolved via
      `iped.parsers.whatsapp.Util` (intentional dependency — UFED chat report reuses
      the WhatsApp visual template + resources).
- [x] `EmailPartyStringBuilder`, `GenericPartyStringBuilder`,
      `TelegramPartyStringBuilder`, `InstagramPartyStringBuilder`,
      `WhatsAppPartyStringBuilder`, `PartyStringBuilderFactory`,
      `OmitEmptyArraysTypeAdapterFactory`, `ConversationConstants`, `HashUtils` moved
      to `iped-parsers-common`; `IItemReader getItem()` added to `IReferencedContact`;
      gson + commons-codec added to the common pom.
- [x] Pre-existing impl errors fixed: `XMLParser` (`HtmlParser` → `JSoupParser`),
      `OFCParser` (`javax.xml.bind` via `jaxb-api:2.3.1`).

## Updates

### 2026-06-21
- Issue created during roadmap consolidation, extracted from `iped-parsers-ROADMAP.md`, status set to `done` based on the original `[x]` marker.
