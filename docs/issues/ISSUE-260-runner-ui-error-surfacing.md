# ISSUE-260: Error surfacing for failed runs

- Status: done
- Roadmap: [iped-runner-ui-ROADMAP.md](../roadmaps/iped-runner-ui-ROADMAP.md)
- Roadmap section: Phase 1 — Run lifecycle UX
- Owner: unassigned
- Created: 2026-06-21
- Updated: 2026-06-21

## Summary

Failed runs render the terminal `message` field in a red-tinted block below the metadata grid for `failed` and `cancelled` statuses, giving the operator an actionable cause instead of a bare status chip.

## Problem

A status chip alone did not tell operators why a run failed, slowing diagnosis.

## Acceptance criteria

- [x] Terminal `message` field is rendered for `failed` and `cancelled` statuses.
- [x] Message is visually distinguished (red-tinted block).

## Updates

### 2026-06-21
- Issue created during roadmap consolidation, extracted from `iped-runner-ui-ROADMAP.md`, status set to `done` based on the original `[x]` marker.
