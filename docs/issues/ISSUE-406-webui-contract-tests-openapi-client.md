# ISSUE-406: Contract tests for the generated OpenAPI client

- Status: done
- Roadmap: [iped-webui-ROADMAP.md](../roadmaps/iped-webui-ROADMAP.md)
- Roadmap section: Phase 4 — Quality bar
- Owner: unassigned
- Created: 2026-06-21
- Updated: 2026-06-21

## Summary

`package.json`'s `test:contract` script runs `tsc --noEmit` over the app sources to catch type drift between island call sites and the generated OpenAPI client types; `generate:python` wraps `openapi-generator-cli` for the Python client.

## Problem

Without a contract check, drift between the backend OpenAPI spec and the generated TypeScript client could silently break islands at runtime rather than at build/CI time.

## Acceptance criteria

- [x] `test:contract` runs `tsc --noEmit` to detect type drift.
- [x] CI fails on drift between island call sites and generated client types.

## Updates

### 2026-06-21
- Issue created during roadmap consolidation, extracted from `iped-webui-ROADMAP.md`, status set to `done` based on the original `[x]` marker.
