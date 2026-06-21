# iped-runner-ui — Evolution Roadmap

> Module purpose: Vite/React frontend bundle for the `iped-runner` Spring Boot dashboard
> (run monitoring, distributed case status from the `iped.status` topic).
> Status legend: see linked issues in docs/issues/ for per-item status (canonical statuses: proposed, planned, in_progress, blocked, done, cancelled).

## Current state (2026-06)
- [ISSUE-256](../issues/ISSUE-256-runner-ui-vite-lib-build.md) — Vite lib build embedded into the runner — `done`
- [ISSUE-257](../issues/ISSUE-257-runner-ui-distributed-cases-display.md) — Distributed cases displayed from the status topic — `done`
- Minimal React 18 app; evolves in lockstep with `iped-runner` (see its `ROADMAP.md` —
  the backend phases drive the UI phases below).

## Phase 1 — Run lifecycle UX (with runner Phase 1) — `done`
- [ISSUE-258](../issues/ISSUE-258-runner-ui-run-detail-view-state-machine.md) — Run detail view with full state machine and metadata — `done`
- [ISSUE-259](../issues/ISSUE-259-runner-ui-live-log-tail.md) — Live log tail per run via SSE — `done`
- [ISSUE-260](../issues/ISSUE-260-runner-ui-error-surfacing.md) — Error surfacing for failed runs — `done`
- [ISSUE-261](../issues/ISSUE-261-runner-ui-auto-refresh-sse-polling.md) — Auto-refresh via SSE/polling instead of manual refresh — `done`

## Phase 2 — Launch and queue management (with runner Phase 2) — `done`
- [ISSUE-262](../issues/ISSUE-262-runner-ui-new-run-form.md) — New-run form with path picker and profile selector — `done`
- [ISSUE-263](../issues/ISSUE-263-runner-ui-queue-view.md) — Queue view with priorities and cancel — `done`
- [ISSUE-264](../issues/ISSUE-264-runner-ui-agent-inventory-panel.md) — Agent inventory panel for distributed runs — `done`

## Phase 3 — Hardening — `in_progress`
- [ISSUE-265](../issues/ISSUE-265-runner-ui-login-flow-role-gating.md) — Login flow and role-gated actions once runner auth lands — `done`
- [ISSUE-266](../issues/ISSUE-266-runner-ui-dashboard-stream-sse.md) — Dashboard stream SSE replaces job-list polling — `done`
- [ISSUE-267](../issues/ISSUE-267-runner-ui-audit-log-tab.md) — Chain-of-custody audit log tab — `done`
- [ISSUE-268](../issues/ISSUE-268-runner-ui-batch-submit-modal.md) — Batch submit modal — `done`
- [ISSUE-269](../issues/ISSUE-269-runner-ui-ci-build-gate.md) — CI build gate for the frontend bundle — `done`
- [ISSUE-270](../issues/ISSUE-270-runner-ui-component-tests.md) — Component tests (Vitest + Testing Library) — `planned`
- [ISSUE-271](../issues/ISSUE-271-runner-ui-typescript-migration.md) — TypeScript migration — `planned`

## Phase 4 — Convergence (5.0) — `done`
- [ISSUE-272](../issues/ISSUE-272-runner-ui-convergence-decision.md) — Convergence decision: standalone ops tool, no merge into iped-webui-server — `done`
- [ISSUE-273](../issues/ISSUE-273-runner-ui-runs-list-filter-search.md) — Runs list filter/search — `done`
- [ISSUE-274](../issues/ISSUE-274-runner-ui-system-health-row.md) — System health row (collapsible) — `done`
- [ISSUE-275](../issues/ISSUE-275-runner-ui-queue-clear-all.md) — Queue clear-all action — `done`
- [ISSUE-276](../issues/ISSUE-276-runner-ui-per-item-audit-drilldown.md) — Per-item audit drill-down — `done`
- [ISSUE-277](../issues/ISSUE-277-runner-ui-full-backend-surface-consumed.md) — All backend surface consumed by the UI — `done`

## Phase 5 — Operator UX polish — `done`
- [ISSUE-278](../issues/ISSUE-278-runner-ui-browser-notifications.md) — Browser notifications on job completion — `done`
- [ISSUE-279](../issues/ISSUE-279-runner-ui-log-download.md) — Log download button — `done`
- [ISSUE-280](../issues/ISSUE-280-runner-ui-keyboard-navigation.md) — Keyboard navigation through the runs list — `done`
- [ISSUE-281](../issues/ISSUE-281-runner-ui-dashboard-toasts.md) — Dashboard toasts — `done`
- [ISSUE-282](../issues/ISSUE-282-runner-ui-session-menu.md) — Session menu with API-key status — `done`

## Progress checks
- [ISSUE-283](../issues/ISSUE-283-runner-ui-progress-check-e2e-run.md) — Progress check: full distributed run lifecycle from the UI — `done`
- [ISSUE-284](../issues/ISSUE-284-runner-ui-progress-check-ci-builds-bundle.md) — Progress check: CI builds the bundle and runner serves hashed assets — `done`
- [ISSUE-285](../issues/ISSUE-285-runner-ui-progress-check-auth-flow.md) — Progress check: auth key flow verified end-to-end — `done`
- [ISSUE-286](../issues/ISSUE-286-runner-ui-progress-check-phase5-parity.md) — Progress check: Phase 5 complete, backend/frontend parity — `done`
