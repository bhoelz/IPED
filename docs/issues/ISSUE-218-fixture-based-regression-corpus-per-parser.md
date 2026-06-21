# ISSUE-218: Fixture-based regression corpus per parser module

- Status: in_progress
- Roadmap: [iped-parsers-ROADMAP.md](../roadmaps/iped-parsers-ROADMAP.md)
- Roadmap section: Phase 2 — Quality and coverage
- Owner: unassigned
- Created: 2026-06-21
- Updated: 2026-06-21

## Summary

Each parser module should carry a small real-world sample-file fixture corpus run in
CI to protect against Tika upgrades and refactors. Most modules already have this;
this round closed the gap for the five recently split modules.

## Problem

The five recently split modules (`whatsapp`, `threema`, `ufed`, `compression`,
and a fifth covered by the same pass) had zero test coverage immediately after the
split, and a real bug (`UfedMessageParserTest` `NoClassDefFoundError`) was hiding
behind that gap.

## Acceptance criteria

- [x] Added baseline `getSupportedTypes()` + `META-INF/services` SPI-discovery smoke
      tests for `whatsapp`, `threema`, `ufed`, `compression`
      (`WhatsAppParserTest`, `ThreemaParserTest`, `UfedParsersTest`,
      `CompressionParsersTest`), matching the convention used for modules without
      real fixtures (e.g. `APKParserTest`).
- [x] Root-caused and fixed the `UfedMessageParserTest` `NoClassDefFoundError`:
      `MediaTypes.MEDIA_TYPE_REGISTRY` in `iped-api` was declared before the
      `MediaType` constants it depends on; reordered so `MEDIA_TYPE_REGISTRY` is
      declared after all `MediaType` constants.
- [ ] Acquire *real* binary fixtures (sqlite DBs, UFED XML) for
      `whatsapp`/`threema`/`ufed` — deferred as a data-acquisition task.

## Updates

### 2026-06-21
- Issue created during roadmap consolidation, extracted from `iped-parsers-ROADMAP.md`, status set to `in_progress` based on the original `[~]` marker.
