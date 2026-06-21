# ISSUE-269: CI build gate for the frontend bundle

- Status: done
- Roadmap: [iped-runner-ui-ROADMAP.md](../roadmaps/iped-runner-ui-ROADMAP.md)
- Roadmap section: Phase 3 — Hardening
- Owner: unassigned
- Created: 2026-06-21
- Updated: 2026-06-21

## Summary

`frontend-maven-plugin` already wires `npm ci` + `npm run build` at the `generate-resources` phase in `iped-runner/pom.xml`, so no additional CI changes were needed to gate the frontend build.

## Problem

The frontend bundle needed to be built and verified as part of the standard Maven CI pipeline, not as a separate manual step.

## Acceptance criteria

- [x] `npm ci` + `npm run build` run at `generate-resources` in `iped-runner/pom.xml`.

## Updates

### 2026-06-21
- Issue created during roadmap consolidation, extracted from `iped-runner-ui-ROADMAP.md`, status set to `done` based on the original `[x]` marker.
