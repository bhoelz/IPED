# iped-app — Evolution Roadmap

> Module purpose: the desktop application and distribution assembly — Swing analysis UI,
> processing UI, timeline graph, bootstrap/CLI entry points, and the release packaging
> (`release.dir` assembly with conf/lib/tools/plugins).
> Status legend: `[ ]` planned · `[~]` in progress · `[x]` done

## Current state (2026-06)
- The largest UI surface in the project: search app, graph renderers, timeline graph
  with cache/persistence, processing progress UI, metadata panels.
- Also owns distribution assembly — two responsibilities in one module.
- 5.0 direction: browser UI becomes the primary analyst interface; the desktop app's
  long-term role is the companion app for native-only capabilities (root roadmap
  workstreams 1–2).

## Phase 1 — Separate assembly from application
- [ ] Extract release/distribution assembly (conf aggregation, lib/tools/plugins layout,
      launcher scripts) into a dedicated packaging module so UI changes don't churn the
      release pipeline.
- [ ] CLI processing entry point (`iped-app` headless mode) decoupled from Swing classes
      — processing a case on a server must not initialize any UI toolkit.

## Phase 2 — Maintenance mode for Swing analysis UI
- [ ] Feature freeze policy: bug fixes and viewer parity support only; new analyst
      features target the browser UI first (document exceptions here when made).
- [ ] Inventory Swing-only features against the web backlog
      (`specs/82-phase-0-swing-inventory.md`) and keep the parity gap list current in
      this file.
- [ ] Keep multi-case/processing UI working as the engine evolves (UIPropertyListener
      decoupling from engine internals).

## Phase 3 — Companion app pivot (5.0 workstream 2)
- [ ] Define the companion-app handoff protocol (open item from browser UI → native
      viewer) — secure local channel, version compatibility checks.
- [ ] Slim companion build: native-dependent viewers (LibreOffice embedding first)
      without the full analysis UI.
- [ ] Installer/signing/update channel for the companion app.

## Phase 4 — Decommission decisions
- [ ] Per-workflow retirement gates: a Swing workflow is removed only after the browser
      equivalent passes parity sign-off on real cases.
- [ ] Final 5.0 shape: browser UI + companion app + headless CLI; full Swing analysis
      app available but deprecated.

## Progress checks
- Headless processing run with no UI toolkit loaded (verify with JVM flags/logs).
- Parity gap list in this file reviewed each release.
