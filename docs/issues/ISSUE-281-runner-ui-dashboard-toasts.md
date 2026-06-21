# ISSUE-281: Dashboard toasts

- Status: done
- Roadmap: [iped-runner-ui-ROADMAP.md](../roadmaps/iped-runner-ui-ROADMAP.md)
- Roadmap section: Phase 5 — Operator UX polish
- Owner: unassigned
- Created: 2026-06-21
- Updated: 2026-06-21

## Summary

A local `useDashToasts()` hook exposes `toast(msg, type)`, called on cancelJob (`cancelled`), clearQueue (`queue cleared (N)`), saveLog (`log saved`), and notification grant (`notifications enabled`), rendered as animated `dash-toast` pills.

## Problem

Several user actions (cancel, clear queue, save log, enable notifications) previously gave no visible confirmation feedback.

## Acceptance criteria

- [x] `useDashToasts()` hook provides a `toast(msg, type)` API.
- [x] Toasts fire on cancelJob, clearQueue, saveLog, and notification grant.

## Updates

### 2026-06-21
- Issue created during roadmap consolidation, extracted from `iped-runner-ui-ROADMAP.md`, status set to `done` based on the original `[x]` marker.
