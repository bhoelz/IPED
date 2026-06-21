# ISSUE-210: Split iped-parser-whatsapp out of iped-parsers-impl

- Status: done
- Roadmap: [iped-parsers-ROADMAP.md](../roadmaps/iped-parsers-ROADMAP.md)
- Roadmap section: Phase 1 — Finish the per-parser split
- Owner: unassigned
- Created: 2026-06-21
- Updated: 2026-06-21

## Summary

WhatsApp parsing (60+ classes, bencode + sqlite deps) was the highest-priority split
candidate out of `iped-parsers-impl` due to its size and self-containedness.

## Problem

WhatsApp parser code lived inside the monolithic `iped-parsers-impl` aggregate instead
of its own Maven module, blocking independent build/versioning and increasing impl's
class count.

## Acceptance criteria

- [x] `iped-parser-whatsapp` module created and compiles clean.
- [x] 28 parser classes + `WhatsAppPartyStringBuilder` + `com.whatsapp.MediaData` +
      CSS/JS/image resources moved.
- [x] `META-INF/services` wired; `iped-parsers-impl` depends on the new module.
- [x] `libfqlite`, `metadata-extractor`, BouncyCastle, Xerces deps declared in the new
      module's pom.
- [x] Report-path helpers inlined into `iped.parsers.whatsapp.Util`.

## Updates

### 2026-06-21
- Issue created during roadmap consolidation, extracted from `iped-parsers-ROADMAP.md`, status set to `done` based on the original `[x]` marker.
