# ISSUE-237: Profile/config selection per run from the dashboard

- Status: done
- Roadmap: [iped-runner-ROADMAP.md](../roadmaps/iped-runner-ROADMAP.md)
- Roadmap section: Phase 2 — Scheduling and queueing
- Owner: unassigned
- Created: 2026-06-21
- Updated: 2026-06-21

## Summary

`RunRequest` gains optional `priority` and `profile` fields, and `QueueController` exposes `GET /profiles` listing `*.toml` files from `runner.profiles-dir` so the dashboard can offer a profile picker.

## Problem

Operators needed to choose a processing profile/config per run from the dashboard instead of relying on a fixed default.

## Acceptance criteria

- [x] `RunRequest` accepts optional `priority` and `profile` fields.
- [x] `GET /profiles` lists available `*.toml` profiles from `runner.profiles-dir`.
- [x] The `--profile <name>` token is injected into CLI args when provided.

## Updates

### 2026-06-21
- Issue created during roadmap consolidation, extracted from `iped-runner-ROADMAP.md`, status set to `done` based on the original `[x]` marker.
