# iped-runner — Evolution Roadmap

> Module purpose: Spring Boot service for launching and monitoring IPED processing —
> local executions and distributed (Kafka) cases — with a web dashboard (bundle built
> from `iped-runner-ui`). Packages: `iped.runner.{config,distributed,execution,web}`.
> Status legend: see linked issues in docs/issues/ for per-item status (canonical statuses: proposed, planned, in_progress, blocked, done, cancelled).

## Current state (2026-06)
- [ISSUE-230](../issues/ISSUE-230-runner-dashboard-distributed-cases.md) — Dashboard shows distributed cases from the `iped.status` topic — `done`
- [ISSUE-231](../issues/ISSUE-231-runner-launch-script-docker-compose.md) — Launch script and docker-compose integration with retry settings — `done`
- Active branch: `feature/kafka-distributed-processing`.

## Phase 1 — Run lifecycle completeness — `done`
- [ISSUE-232](../issues/ISSUE-232-runner-run-state-machine-api.md) — Full run state machine surfaced in the API — `done`
- [ISSUE-233](../issues/ISSUE-233-runner-run-history-persistence.md) — Run history persistence across runner restarts — `done`
- [ISSUE-234](../issues/ISSUE-234-runner-log-streaming-per-run.md) — Log streaming per run — `done`
- [ISSUE-235](../issues/ISSUE-235-runner-cancel-abort-run.md) — Cancel/abort a run cleanly — `done`

## Phase 2 — Scheduling and queueing — `done`
- [ISSUE-236](../issues/ISSUE-236-runner-case-queue-priorities-concurrency.md) — Case queue with priorities and concurrency limits — `done`
- [ISSUE-237](../issues/ISSUE-237-runner-profile-selection-per-run.md) — Profile/config selection per run from the dashboard — `done`
- [ISSUE-238](../issues/ISSUE-238-runner-batch-submission-folder.md) — Batch submission for a folder of evidence — `done`

## Phase 3 — Operations — `done`
- [ISSUE-239](../issues/ISSUE-239-runner-api-authn-authz.md) — AuthN/authZ for the runner API — `done`
- [ISSUE-240](../issues/ISSUE-240-runner-metrics-endpoint.md) — Metrics endpoint — `done`
- [ISSUE-241](../issues/ISSUE-241-runner-webhook-notifications.md) — Webhook notifications on run completion/failure — `done`
- [ISSUE-242](../issues/ISSUE-242-runner-multi-node-agent-inventory-view.md) — Multi-node awareness: agent inventory view with health/heartbeats — `done`

## Phase 4 — 5.0 integration — `done`
- [ISSUE-243](../issues/ISSUE-243-runner-control-plane-api.md) — Become the control-plane API of the 5.0 target architecture — `done`
- [ISSUE-244](../issues/ISSUE-244-runner-webapi-job-contract-alignment.md) — Contract alignment with iped-webapi job endpoints — `done`

## Phase 5 — Kafka security and chain-of-custody audit — `done`
- [ISSUE-245](../issues/ISSUE-245-runner-agent-heartbeat-inventory-service.md) — Agent heartbeat inventory service — `done`
- [ISSUE-246](../issues/ISSUE-246-runner-statusevent-agentid-field.md) — agentId field added to StatusEvent for provenance — `done`
- [ISSUE-247](../issues/ISSUE-247-runner-kafka-security-configurer.md) — KafkaSecurityConfigurer for TLS/SASL client properties — `done`
- [ISSUE-248](../issues/ISSUE-248-runner-payload-signer-hmac.md) — PayloadSigner HMAC-SHA256 for chain-of-custody integrity — `done`
- [ISSUE-249](../issues/ISSUE-249-runner-processing-record-chain-of-custody.md) — ProcessingRecord immutable chain-of-custody record — `done`
- [ISSUE-250](../issues/ISSUE-250-runner-processing-audit-log.md) — ProcessingAuditLog with CSV export and optional persistence — `done`
- [ISSUE-251](../issues/ISSUE-251-runner-audit-controller-endpoints.md) — AuditController endpoints for chain-of-custody records — `done`

## Phase 5 — Multi-node agent inventory — `done`
- [ISSUE-252](../issues/ISSUE-252-runner-multi-node-agent-inventory.md) — Multi-node agent inventory (health/heartbeats) delivered — `done`

## Progress checks
- A distributed run launched, monitored, and completed entirely from the dashboard.
- Runner restart mid-run → state recovered, run still tracked to completion.
