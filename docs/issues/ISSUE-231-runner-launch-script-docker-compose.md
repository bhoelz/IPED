# ISSUE-231: Launch script and docker-compose integration with retry settings

- Status: done
- Roadmap: [iped-runner-ROADMAP.md](../roadmaps/iped-runner-ROADMAP.md)
- Roadmap section: Current state
- Owner: unassigned
- Created: 2026-06-21
- Updated: 2026-06-21

## Summary

`start-iped-runner.ps1` and the docker-compose service definition give operators a reliable way to bring up the runner with retry settings configured out of the box.

## Problem

The runner needed a documented, repeatable launch path including container orchestration and retry behavior.

## Acceptance criteria

- [x] `start-iped-runner.ps1` launch script exists.
- [x] docker-compose integration includes retry settings.

## Updates

### 2026-06-21
- Issue created during roadmap consolidation, extracted from `iped-runner-ROADMAP.md`, status set to `done` based on the original `[x]` marker.
