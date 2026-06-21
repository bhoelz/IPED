# ISSUE-272: Convergence decision — standalone ops tool, no merge into iped-webui-server

- Status: done
- Roadmap: [iped-runner-ui-ROADMAP.md](../roadmaps/iped-runner-ui-ROADMAP.md)
- Roadmap section: Phase 4 — Convergence (5.0)
- Owner: unassigned
- Created: 2026-06-21
- Updated: 2026-06-21

## Summary

Decision recorded: the runner dashboard stays as a standalone ops tool rather than merging into `iped-webui-server`, since the runner is a self-contained control plane with its own auth, queue, and audit trail independent of individual case access.

## Problem

The project needed a clear, documented decision on whether the runner dashboard should converge with the main web UI server or remain independent, to avoid duplicated or conflicting effort.

## Acceptance criteria

- [x] Decision documented: no merge; runner dashboard remains standalone.

## Updates

### 2026-06-21
- Issue created during roadmap consolidation, extracted from `iped-runner-ui-ROADMAP.md`, status set to `done` based on the original `[x]` marker.
