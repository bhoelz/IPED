# ISSUE-429: Packaging — single deployable documented in a DEPLOYMENT-GUIDE.md

- Status: done
- Roadmap: [iped-webui-server-ROADMAP.md](../roadmaps/iped-webui-server-ROADMAP.md)
- Roadmap section: Phase 4 — Production posture (5.0)
- Owner: unassigned
- Created: 2026-06-21
- Updated: 2026-07-16

## Summary

`DEPLOYMENT-GUIDE.md` should document build commands, configuration reference, an nginx TLS termination example, a Docker/Compose quick-start, an observability section, and security notes for `iped-webui-server`.

## Problem

Operators need a single reference document covering how to build, configure, and deploy this module in production, including TLS termination guidance.

## Acceptance criteria

- [ ] `iped-webui-server/DEPLOYMENT-GUIDE.md` exists and covers build, configuration, TLS termination, Docker/Compose, observability, and security notes.

## Updates

### 2026-06-21
- Issue created during roadmap consolidation, extracted from `iped-webui-server-ROADMAP.md`, status set to `done` based on the original `[x]` marker.
- Status corrected to `planned`: while triaging `docs/needs-revision/`, verified no `DEPLOYMENT-GUIDE.md` actually exists anywhere under `iped-webui-server/`. The sibling module `iped-distributed/DEPLOYMENT-GUIDE.md` has the matching nginx/Docker/observability/security shape described here, which is likely what got cross-attributed during the original roadmap extraction. No file was lost — it appears it was never written for this module.

### 2026-07-16
- Added the canonical `iped-webui-server/DEPLOYMENT-GUIDE.md` covering build, configuration, TLS, Docker/Compose, observability, and security.
