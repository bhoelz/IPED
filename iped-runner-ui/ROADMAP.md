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
- [ ] Run detail view: full state machine (queued/running/completed/failed/timed-out),
      timings, per-stage progress, retry counts.
- [ ] Live log tail per run (stream from the runner's log endpoint).
- [ ] Error surfacing: failed runs show the actionable cause, not just a status chip.
- [ ] Auto-refresh via SSE/websocket instead of polling (when the runner exposes it).

## Phase 2 — Launch and queue management (with runner Phase 2)
- [ ] New-run form: evidence path picker, TOML profile selector with pre-launch
      validation feedback.
- [ ] Queue view with priorities, reorder, cancel.
- [ ] Agent inventory panel (health, current segment, lag) for distributed runs.

## Phase 3 — Hardening
- [ ] Login flow once runner auth lands; role-gated launch/cancel actions.
- [ ] Component tests (Vitest + Testing Library) and a CI build gate (`npm run build`
      as part of the runner module build).
- [ ] Consider TypeScript migration while the codebase is still small.

## Phase 4 — Convergence (5.0)
- [ ] Revisit overlap with the main web UI: if the runner becomes the control-plane API
      consumed by `iped-webui-server`, this dashboard either stays as a thin ops tool or
      merges into the main UI — decide with `iped-runner` Phase 4 and record here.

## Progress checks
- Launch → monitor → completion of a distributed run entirely from this UI.
- CI builds the bundle and the runner serves the hashed assets.
