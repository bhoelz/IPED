# ISSUE-335: Decide iped-ui's fate — fold into web UI, keep standalone, or retire

- Status: planned
- Roadmap: [iped-ui-ROADMAP.md](../roadmaps/iped-ui-ROADMAP.md)
- Roadmap section: Phase 0 — Strategic decision (blocks everything else)
- Owner: unassigned
- Created: 2026-06-21
- Updated: 2026-06-21

## Summary

Make an explicit decision about the future of the standalone `iped-ui` configuration web app: fold it into the SSR web UI as fragments/islands, keep it as a standalone admin tool, or retire it in favor of editing TOML files directly with schema validation. The recommendation leans toward folding it into `iped-webui-server` for a single web stack, auth model, and deployment.

## Problem

`iped-ui` currently overlaps conceptually with the main web UI effort (`iped-webui`/`iped-webui-server`) and the runner dashboard's profile-selection plans, with no decision recorded on which direction to take, blocking all other work on this module.

## Acceptance criteria

- [ ] Decision made among: (a) fold into SSR web UI as fragments/islands, (b) keep as standalone admin tool, (c) retire in favor of direct TOML editing with schema validation.
- [ ] Decision and rationale recorded with a date in the roadmap.
- [ ] Downstream phases (interim hardening vs. migration) aligned to the chosen direction.

## Updates

### 2026-06-21
- Issue created during roadmap consolidation, extracted from `iped-ui-ROADMAP.md`, status set to `planned` based on the original `[ ]` marker.
