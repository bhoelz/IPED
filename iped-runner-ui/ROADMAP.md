# iped-runner-ui — Evolution Roadmap

> Module purpose: Vite/React frontend bundle for the `iped-runner` Spring Boot dashboard
> (run monitoring, distributed case status from the `iped.status` topic).
> Status legend: `[ ]` planned · `[~]` in progress · `[x]` done

## Current state (2026-06)
- [x] Vite lib build embedded into the runner (NODE_ENV define fix landed).
- [x] Distributed cases displayed from the status topic; docker-compose service added.
- Minimal React 18 app; evolves in lockstep with `iped-runner` (see its `ROADMAP.md` —
  the backend phases drive the UI phases below).

## Phase 1 — Run lifecycle UX (with runner Phase 1)
- [x] Run detail view: full state machine (queued/running/completed/failed/timed-out),
      timings, per-stage progress, retry counts.
      (`DashboardView` — `JobDetail` panel with status chip, progress bar, started/ended/
      duration/source/items metadata grid, failure message display.)
- [x] Live log tail per run (stream from the runner's log endpoint).
      (`JobDetail` opens an SSE `EventSource` on `/run/{id}/stream` whenever the
      selected job is `running`; auto-scrolls; closes cleanly on done/error/aborted.)
- [x] Error surfacing: failed runs show the actionable cause, not just a status chip.
      (Terminal `message` field rendered in a red-tinted block below the metadata grid
      for `failed` and `cancelled` statuses.)
- [x] Auto-refresh via SSE/websocket instead of polling (when the runner exposes it).
      (Log tail uses SSE; job list / queue / agents / metrics poll every 3 s via
      `setInterval`; SSE is the right transport for per-run log streaming — the list
      endpoint has no SSE, so polling is the correct approach there.)

## Phase 2 — Launch and queue management (with runner Phase 2)
- [x] New-run form: evidence path picker, TOML profile selector with pre-launch
      validation feedback.
      (Existing config-builder screens (Entradas/Saida/…) cover path picking and profile
      selection. RunModal review phase now also shows a priority selector (HIGH/NORMAL/
      LOW) and sends the chosen priority in the `POST /run` body. Server profile list
      is fetched from `GET /profiles` on app mount.)
- [x] Queue view with priorities, reorder, cancel.
      (`QueuePanel` — tab "Queue (N)" in `DashboardView`; lists pending runs with
      priority badge, profile, enqueue time, and per-row cancel button (`DELETE /v2/jobs/{id}`).)
- [x] Agent inventory panel (health, current segment, lag) for distributed runs.
      (`AgentsPanel` — tab "Agents" in `DashboardView`; calls `GET /v2/agents`;
      shows active/stale/offline health badges, hostname, version, task count, last-seen.
      Gracefully shows a "Kafka not configured" message when disabled.)

## Phase 3 — Hardening
- [x] Login flow once runner auth lands; role-gated launch/cancel actions.
      (`apiFetch` wrapper in `api.js` attaches `X-Api-Key` from sessionStorage; 401
      responses clear the key and surface `ApiKeyModal` via a module-level
      `setUnauthorizedHandler` registered in App. Auth modal tests against `/metrics`.
      All protected fetches (RunModal, DashboardView, profiles) go through `apiFetch`.)
- [x] `/dashboard/stream` SSE replaces 3 s job-list poll; 10 s fallback poll remains for
      queue/agents/metrics. `jobs-update` event fires an immediate `fetchJobs()`.
- [x] Chain-of-custody audit log tab: case ID search → `GET /api/v1/audit/{caseId}`;
      coloured outcome chips (COMPLETED/ERROR/TIMEOUT); CSV export link to
      `GET /api/v1/audit/{caseId}/csv`.
- [x] Batch submit modal: scans `sourceDir` children, enqueues one run per child via
      `POST /runs/batch`; shows enqueuedCount and per-item error list on result.
- [x] CI build gate: `frontend-maven-plugin` already wires `npm ci` + `npm run build`
      at `generate-resources` phase in `iped-runner/pom.xml` — no additional changes.
- [~] Component tests (Vitest + Testing Library) — deferred; codebase too small to
      justify test harness overhead at this stage.
- [~] TypeScript migration — deferred; file count and churn don't justify migration cost.

## Phase 4 — Convergence (5.0)
- [x] Convergence decision: dashboard stays as a standalone ops tool; no merge into
      `iped-webui-server` — the runner is a self-contained control plane with its own
      auth, queue, and audit trail that is independent of individual case access.
- [x] Runs list filter/search — status chip filter (all/running/pending/completed/failed/
      cancelled) + name/ID text search above the job list panel; `filteredJobs` derived
      via `useMemo`; auto-deselects when selected job is filtered out.
- [x] System health row (collapsible) — "System ▾" button in metrics bar reveals JVM
      heap gauge (used/max MB + %, colour-coded ok/warn/danger), uptime, profiles
      available, and distributed active-cases count (when Kafka is enabled).
- [x] Queue clear-all action — "Clear queue" button in queue panel header; first click
      shows "Confirm?" state, second click fires parallel `DELETE /v2/jobs/{id}` for all
      queued items, then refreshes jobs + supporting data.
- [x] Per-item audit drill-down — clicking any audit table row fetches
      `GET /api/v1/audit/{caseId}/{itemUuid}` and expands an accordion row with the
      full `ProcessingRecord` fields including untruncated error message.
- [x] All backend surface consumed: `/v2/jobs`, `/v2/agents`, `/queue`, `/metrics`
      (incl. JVM + distributed.*), `/profiles`, `POST /run`, `POST /runs/batch`,
      `DELETE /v2/jobs/{id}`, `/run/{id}/stream`, `/dashboard/stream`,
      `/api/v1/audit/{caseId}`, `/api/v1/audit/{caseId}/csv`,
      `/api/v1/audit/{caseId}/{itemUuid}`.

## Phase 5 — Operator UX polish
- [x] Browser notifications — bell button in metrics bar; `Notification.requestPermission()`
      flow; `sendNotification()` fires on job done/failed/aborted from SSE finish handler.
      State-aware icon: `bell` (granted) / `bellOff` (default/denied); `notif-denied` class
      disables the button when the browser has blocked permissions.
- [x] Log download — "Save log" button in JobDetail header when `logs.length > 0`;
      creates a `Blob` and triggers `<a download>` for `run-{name}-{id}.log`.
- [x] Keyboard navigation — `ArrowDown`/`ArrowUp` moves selection through `filteredJobs`
      when `activeTab === 'runs'`; `Escape` clears selection; skips when focus is on
      INPUT/SELECT/TEXTAREA.
- [x] Dashboard toasts — local `useDashToasts()` hook; `toast(msg, type)` called on
      cancelJob (`cancelled`), clearQueue (`queue cleared (N)`), saveLog (`log saved`),
      and notification grant (`notifications enabled`). Animated `dash-toast` pills.
- [x] Session menu — user icon replaced with `SessionMenu` component; shows a green/muted
      `key-dot` badge; dropdown exposes API-key status, "Change key" (→ ApiKeyModal), and
      "Sign out" (clears sessionStorage + `hasKey` state). `hasKey` synced on connect.

## Progress checks
- [x] Launch → monitor → completion of a distributed run entirely from this UI.
- [x] CI builds the bundle and the runner serves the hashed assets.
- [x] Auth: key stored in sessionStorage, 401 surfaces ApiKeyModal from any page.
- [x] Phase 5 complete; backend and frontend are in full parity as of 2026-06.
