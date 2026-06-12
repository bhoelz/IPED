# iped-runner — Evolution Roadmap

> Module purpose: Spring Boot service for launching and monitoring IPED processing —
> local executions and distributed (Kafka) cases — with a web dashboard (bundle built
> from `iped-runner-ui`). Packages: `iped.runner.{config,distributed,execution,web}`.
> Status legend: `[ ]` planned · `[~]` in progress · `[x]` done

## Current state (2026-06)
- [x] Dashboard shows distributed cases from the `iped.status` topic.
- [x] Launch script (`start-iped-runner.ps1`) and docker-compose integration with retry
      settings.
- Active branch: `feature/kafka-distributed-processing`.

## Phase 1 — Run lifecycle completeness
- [ ] Full run state machine surfaced in the API: queued → running → completed/failed/
      timed-out, with the timeout/completion detection already built in the distributed
      module reflected accurately.
- [ ] Run history persistence (survive runner restarts; today's state is in-memory/topic
      derived) — small embedded DB or compacted topic, decision recorded here.
- [ ] Log streaming per run (tail engine/agent logs through the dashboard).
- [ ] Cancel/abort a run cleanly (coordinate with distributed graceful-drain work).

## Phase 2 — Scheduling and queueing
- [ ] Case queue with priorities and concurrency limits (delegating to the engine's
      ProcessingOrchestrator for local runs, the coordinator for distributed runs —
      one queue abstraction over both).
- [ ] Profile/config selection per run from the dashboard (TOML profile picker with
      validation before launch).
- [ ] Batch submission: point at a folder of evidence, generate N runs.

## Phase 3 — Operations
- [ ] AuthN/authZ for the dashboard and API (launching processing is a privileged op).
- [ ] Metrics endpoint (runs in flight, agent health, Kafka lag passthrough) for the
      observability stack.
- [ ] Notifications on completion/failure (webhook first; mail later).
- [ ] Multi-node awareness: agent inventory view with health/heartbeats.

## Phase 4 — 5.0 integration
- [ ] Become the control-plane API of the root roadmap's target architecture (case/job
      orchestration), consumed by the browser UI and MCP `job_*` tools rather than
      having its own parallel UI surface long-term.
- [ ] Contract alignment with `iped-webapi` job endpoints (one job model, not two).

## Progress checks
- A distributed run launched, monitored, and completed entirely from the dashboard.
- Runner restart mid-run → state recovered, run still tracked to completion.
