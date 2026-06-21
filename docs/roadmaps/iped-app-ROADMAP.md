# iped-app — Evolution Roadmap

> Module purpose: the desktop application and distribution assembly — Swing analysis UI,
> processing UI, timeline graph, bootstrap/CLI entry points, and the release packaging
> (`release.dir` assembly with conf/lib/tools/plugins).
> Status legend: see linked issues in docs/issues/ for per-item status (canonical statuses: proposed, planned, in_progress, blocked, done, cancelled).

## Current state (2026-06)
- The largest UI surface in the project: search app, graph renderers, timeline graph
  with cache/persistence, processing progress UI, metadata panels.
- Also owns distribution assembly — two responsibilities in one module.
- 5.0 direction: browser UI becomes the primary analyst interface; the desktop app's
  long-term role is the companion app for native-only capabilities (root roadmap
  workstreams 1–2).

## Phase 1 — Separate assembly from application — `in_progress`
- [ISSUE-033](../issues/ISSUE-033-extract-release-assembly-module.md) — Extract release/distribution assembly into a dedicated packaging module — `planned`
- [ISSUE-034](../issues/ISSUE-034-decouple-cli-bootstrap-from-swing.md) — Decouple CLI processing entry point from Swing classes — `done`

## Phase 2 — Maintenance mode for Swing analysis UI — `in_progress`
- [ISSUE-035](../issues/ISSUE-035-swing-feature-freeze-policy.md) — Adopt feature freeze policy for the Swing analysis UI — `done`
- [ISSUE-036](../issues/ISSUE-036-swing-web-parity-gap-inventory.md) — Inventory Swing-only features against the web backlog (parity gap list) — `done`
- [ISSUE-037](../issues/ISSUE-037-multicase-ui-engine-coupling.md) — Keep multi-case/processing UI working as the engine evolves — `planned`

## Phase 3 — Companion app pivot (5.0 workstream 2) — `in_progress`
- [ISSUE-038](../issues/ISSUE-038-companion-handoff-protocol.md) — Define companion-app handoff protocol — `done`
- [ISSUE-039](../issues/ISSUE-039-slim-companion-build.md) — Slim companion build with native-dependent viewers only — `planned`
- [ISSUE-040](../issues/ISSUE-040-companion-app-installer-update-channel.md) — Installer/signing/update channel for the companion app — `planned`

## Phase 4 — Decommission decisions — `planned`
- [ISSUE-041](../issues/ISSUE-041-swing-workflow-retirement-gates.md) — Per-workflow retirement gates for Swing workflows — `planned`
- [ISSUE-042](../issues/ISSUE-042-final-5.0-shape-decision.md) — Finalize 5.0 shape — browser UI, companion app, headless CLI — `planned`

## Progress checks
- Headless processing run with no UI toolkit loaded (verify with JVM flags/logs).
- Parity gap list in this file reviewed each release.
