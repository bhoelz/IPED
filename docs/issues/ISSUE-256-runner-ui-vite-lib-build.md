# ISSUE-256: Vite lib build embedded into the runner

- Status: done
- Roadmap: [iped-runner-ui-ROADMAP.md](../roadmaps/iped-runner-ui-ROADMAP.md)
- Roadmap section: Current state
- Owner: unassigned
- Created: 2026-06-21
- Updated: 2026-06-21

## Summary

The Vite library build is embedded into the `iped-runner` Spring Boot module, with the `NODE_ENV` define fix landed so the bundle behaves correctly in production.

## Problem

The frontend build needed to be reliably embedded into the backend module's build pipeline rather than built/served separately.

## Acceptance criteria

- [x] Vite lib build is embedded into the runner module's build.
- [x] `NODE_ENV` define fix is applied.

## Updates

### 2026-06-21
- Issue created during roadmap consolidation, extracted from `iped-runner-ui-ROADMAP.md`, status set to `done` based on the original `[x]` marker.
