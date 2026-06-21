# ISSUE-274: System health row (collapsible)

- Status: done
- Roadmap: [iped-runner-ui-ROADMAP.md](../roadmaps/iped-runner-ui-ROADMAP.md)
- Roadmap section: Phase 4 — Convergence (5.0)
- Owner: unassigned
- Created: 2026-06-21
- Updated: 2026-06-21

## Summary

A "System ▾" button in the metrics bar reveals a JVM heap gauge (used/max MB and percentage, colour-coded ok/warn/danger), uptime, available profiles, and the distributed active-cases count when Kafka is enabled.

## Problem

JVM and distributed health metrics existed in the backend but had no compact UI surface.

## Acceptance criteria

- [x] Collapsible "System" row shows JVM heap gauge, uptime, profiles available.
- [x] Shows distributed active-cases count when Kafka is enabled.

## Updates

### 2026-06-21
- Issue created during roadmap consolidation, extracted from `iped-runner-ui-ROADMAP.md`, status set to `done` based on the original `[x]` marker.
