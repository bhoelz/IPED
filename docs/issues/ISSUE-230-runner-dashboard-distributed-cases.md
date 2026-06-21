# ISSUE-230: Dashboard shows distributed cases from the iped.status topic

- Status: done
- Roadmap: [iped-runner-ROADMAP.md](../roadmaps/iped-runner-ROADMAP.md)
- Roadmap section: Current state
- Owner: unassigned
- Created: 2026-06-21
- Updated: 2026-06-21

## Summary

The runner dashboard surfaces distributed case status sourced from the `iped.status` Kafka topic, giving operators visibility into in-flight distributed processing without a separate tool.

## Problem

Operators needed a single place to observe distributed case progress without manually inspecting the Kafka topic.

## Acceptance criteria

- [x] Dashboard reads and renders status events from the `iped.status` topic.

## Updates

### 2026-06-21
- Issue created during roadmap consolidation, extracted from `iped-runner-ROADMAP.md`, status set to `done` based on the original `[x]` marker.
