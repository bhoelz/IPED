# ISSUE-214: Split iped-parser-compression out of iped-parsers-impl

- Status: done
- Roadmap: [iped-parsers-ROADMAP.md](../roadmaps/iped-parsers-ROADMAP.md)
- Roadmap section: Phase 1 — Finish the per-parser split
- Owner: unassigned
- Created: 2026-06-21
- Updated: 2026-06-21

## Summary

Compression-format parsing (`SevenZipParser`, `RARParser`, `LZFSEParser`,
`PackageParser`) was split out of `iped-parsers-impl` into its own Maven module.

## Problem

Compression parser code lived inside the monolithic `iped-parsers-impl` aggregate,
and a shared path helper had a circular dependency back into impl.

## Acceptance criteria

- [x] `iped-parser-compression` module created and compiles clean.
- [x] `SevenZipParser`, `RARParser`, `LZFSEParser`, `PackageParser` + `RawISOConverter`
      moved; `META-INF/services` wired for 4 parsers.
- [x] `iped.parsers.util.Util.getParentPath` replaced by local
      `CompressionUtil.getParentPath` (removing the impl circular dependency).
- [x] sevenzipjbinding, junrar, commons-compress, RagingMoose deps declared.

## Updates

### 2026-06-21
- Issue created during roadmap consolidation, extracted from `iped-parsers-ROADMAP.md`, status set to `done` based on the original `[x]` marker.
