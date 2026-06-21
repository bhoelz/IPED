# ISSUE-099: Finish TOML migration cleanup (remove .txt fallback paths)

- Status: done
- Roadmap: [iped-engine-core-ROADMAP.md](../roadmaps/iped-engine-core-ROADMAP.md)
- Roadmap section: Phase 2 — Configuration system completion
- Owner: unassigned
- Created: 2026-06-21
- Updated: 2026-06-21

## Summary

Remove any remaining `.txt` fallback/parsing paths now that all profiles have converted to TOML.

## Problem

Legacy `.txt` config parsing paths needed to be confirmed fully removed once the TOML migration completed, to avoid dual config formats lingering in the codebase.

## Acceptance criteria

- [x] Confirmed `Configuration.java:54`'s `ipedRoot.txt` reference is a legacy path-locator, not a config-parser.
- [x] No TOML fallback `.txt` parsing paths remain in `iped-engine-core`.

## Updates

### 2026-06-21
- Issue created during roadmap consolidation, extracted from `iped-engine-core-ROADMAP.md`, status set to `done`.
