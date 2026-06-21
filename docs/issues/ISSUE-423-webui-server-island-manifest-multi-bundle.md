# ISSUE-423: Island manifest supports multiple bundles with independent hashing

- Status: done
- Roadmap: [iped-webui-server-ROADMAP.md](../roadmaps/iped-webui-server-ROADMAP.md)
- Roadmap section: Phase 2 — Island growth
- Owner: unassigned
- Created: 2026-06-21
- Updated: 2026-06-21

## Summary

`IslandManifest.java` loads `manifest.json` (written by `generate-islands-manifest.mjs`) and exposes `chunkFor(islandName)` for targeted `<link rel="modulepreload">` injection, so each island's chunk is hashed and cached independently.

## Problem

The server needed to know the hashed URL for each island's chunk so it could inject precise preload hints without coupling to a single combined bundle.

## Acceptance criteria

- [x] `IslandManifest.chunkFor(islandName)` returns the hashed chunk URL for that island.
- [x] Updating one island's bundle does not bust the cache for others.

## Updates

### 2026-06-21
- Issue created during roadmap consolidation, extracted from `iped-webui-server-ROADMAP.md`, status set to `done` based on the original `[x]` marker.
