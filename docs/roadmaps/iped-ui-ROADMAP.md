# iped-ui — Evolution Roadmap

> Module purpose: React-based configuration web UI talking to the JAX-RS Configuration
> API on embedded Jetty — editing IPED's (now TOML-based) configuration in a browser.
> Status legend: see linked issues in docs/issues/ for per-item status (canonical statuses: proposed, planned, in_progress, blocked, done, cancelled).

## Current state (2026-06)
- Standalone React app (npm workspace, not in the Maven reactor) targeting the
  ConfigurationServer at `localhost:8080`.
- Overlaps conceptually with the main web UI effort (`iped-webui`/`iped-webui-server`)
  and with the runner dashboard's profile-selection plans.

## Phase 0 — Strategic decision (blocks everything else) — `planned`
- [ISSUE-335](../issues/ISSUE-335-decide-config-ui-module-fate.md) — Decide iped-ui's fate — fold into web UI, keep standalone, or retire — `planned`

## Phase 1 — If kept (interim hardening) — `planned`
- [ISSUE-336](../issues/ISSUE-336-config-ui-align-toml-deviation-layer.md) — Align config UI with TOML deviation-layer model — `planned`
- [ISSUE-337](../issues/ISSUE-337-config-ui-schema-driven-forms.md) — Schema-driven config forms generated from engine-core schema — `planned`
- [ISSUE-338](../issues/ISSUE-338-config-ui-shared-validation.md) — Validate config saves using the same engine-side validators — `planned`

## Phase 2 — Migration (if decision = fold into web UI) — `planned`
- [ISSUE-339](../issues/ISSUE-339-config-ui-port-ssr-fragments.md) — Port configuration screens as SSR fragments under iped-webui-server — `planned`
- [ISSUE-340](../issues/ISSUE-340-config-ui-retire-standalone-workspace.md) — Retire the standalone iped-ui workspace — `planned`

## Progress checks
- Decision recorded here with date and rationale.
- No second source of config-editing truth once the decision is executed.
