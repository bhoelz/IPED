# ISSUE-397: Island build pipeline with per-island independent hashes

- Status: done
- Roadmap: [iped-webui-ROADMAP.md](../roadmaps/iped-webui-ROADMAP.md)
- Roadmap section: Phase 1 — Island library consolidation
- Owner: unassigned
- Created: 2026-06-21
- Updated: 2026-06-21

## Summary

`main.ts` now uses dynamic `import()` per island so the bundler emits one hashed chunk per island; `scripts/generate-islands-manifest.mjs` writes `dist/islands/manifest.json` mapping island names to chunk URLs for the SSR server.

## Problem

A single combined bundle meant updating any one island invalidated the cache for all islands; per-island hashing was needed.

## Acceptance criteria

- [x] Each island is dynamically imported and emitted as an independently hashed chunk.
- [x] `generate-islands-manifest.mjs` produces `manifest.json` with `{entrypoint, chunks}`.
- [x] `build:islands` (production) and `build:islands:dev` (unminified, source maps) scripts exist.

## Updates

### 2026-06-21
- Issue created during roadmap consolidation, extracted from `iped-webui-ROADMAP.md`, status set to `done` based on the original `[x]` marker.
