# ISSUE-339: Port configuration screens as SSR fragments under iped-webui-server

- Status: planned
- Roadmap: [iped-ui-ROADMAP.md](../roadmaps/iped-ui-ROADMAP.md)
- Roadmap section: Phase 2 — Migration (if decision = fold into web UI)
- Owner: unassigned
- Created: 2026-06-21
- Updated: 2026-06-21

## Summary

If the strategic decision is to fold configuration editing into the main web UI, port the configuration screens as SSR fragments (HTMX forms suit config editing well) under `iped-webui-server`, reusing the schema-driven approach.

## Problem

Maintaining configuration editing as a separate standalone app duplicates web stack, auth, and deployment concerns once the decision is made to consolidate into one web UI.

## Acceptance criteria

- [ ] Configuration screens reimplemented as SSR fragments (HTMX) under `iped-webui-server`.
- [ ] Schema-driven form generation approach reused from the standalone app.

## Updates

### 2026-06-21
- Issue created during roadmap consolidation, extracted from `iped-ui-ROADMAP.md`, status set to `planned` based on the original `[ ]` marker.
