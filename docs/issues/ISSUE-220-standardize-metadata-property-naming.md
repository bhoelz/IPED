# ISSUE-220: Standardize chat-message metadata property naming across parsers

- Status: done
- Roadmap: [iped-parsers-ROADMAP.md](../roadmaps/iped-parsers-ROADMAP.md)
- Roadmap section: Phase 2 — Quality and coverage
- Owner: unassigned
- Created: 2026-06-21
- Updated: 2026-06-21

## Summary

An audit of metadata property naming across parsers found that `WhatsAppParser`,
`ThreemaParser`, `SkypeParser`, and `TelegramParser` independently hand-typed
identical literal property names for the same semantic chat-message concepts, with
no compiler safety net against drift.

## Problem

Hand-typed literal strings (`"chatId"`, `"mediaName"`, `"mediaMime"`,
`"mediaSize"`, `"duration"`, `"messageStatus"`) duplicated across four parsers meant
a future typo or casing change in one of them would silently fragment a single
logical index property into two fields.

## Acceptance criteria

- [x] Audit confirmed no typo/casing drift against canonical
      `ExtraProperties`/`BasicProps` constants.
- [x] Added `CHAT_ID`/`MEDIA_NAME`/`MEDIA_MIME`/`MEDIA_SIZE`/`MESSAGE_DURATION`/
      `MESSAGE_STATUS` constants to `iped-parsers-common`'s `ConversationConstants`
      (same string values, zero behavior change).
- [x] Switched all four parsers' `metadata.set/add` call sites to reference the new
      constants.
- [x] Left DB-column-name literals (e.g. `rs.getString("mediaName")`) and
      `SkypeParser`'s `"sendingStatus"` untouched as out-of-scope/non-cross-parser
      concerns.

## Updates

### 2026-06-21
- Issue created during roadmap consolidation, extracted from `iped-parsers-ROADMAP.md`, status set to `done` based on the original `[x]` marker.
