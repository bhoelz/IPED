# iped-distributed

Kafka-based distributed processing — coordinator, processing agents, status reporting (`iped.distributed.{agent,config,coordinator,kafka,status}`). Implements workstream 4 of the root [ROADMAP.md](../../ROADMAP.md).

## Roadmap

See [iped-distributed-ROADMAP.md](../../docs/roadmaps/iped-distributed-ROADMAP.md) for planned work, current phase status, and linked issues.

## Operations & Monitoring

### Deployment
- [DEPLOYMENT-GUIDE.md](DEPLOYMENT-GUIDE.md) — operational deployment guide (Kafka setup, security hardening, health checks, monitoring, rollout checklist).

### Observability
- **Prometheus alerting rules** — three live alert rules for pipeline failure modes (consumer lag, DLQ overflow, case stalled); see [`observability/alerts.rules.yml`](observability/alerts.rules.yml).
- **Runbooks** (cross-referenced from alert rules):
  - [consumer-lag-growing.md](../../.claude/sdlc/runbooks/consumer-lag-growing.md) — diagnosis and remediation for slow task agents
  - [dlq-overflow.md](../../.claude/sdlc/runbooks/dlq-overflow.md) — handling repeated item-processing failures
  - [case-stalled.md](../../.claude/sdlc/runbooks/case-stalled.md) — detecting and recovering stalled cases

### Coordinator API
Key endpoints (full API in DEPLOYMENT-GUIDE §10–11):
- `GET /api/v1/health` — readiness probe with Kafka broker connectivity status
- `GET /metrics` — Prometheus metrics (items processed, lag, DLQ depth, case stall status)
- `GET /api/v1/agents` — list registered task agents
- `GET /api/v1/cases/{caseId}` — case status
- `GET /api/v1/dlq/{caseId}` — dead-letter queue inspection and management

## Further reading

- [PROCESSING.md](PROCESSING.md) — original design rationale (Kafka topic layout, item serialization, task-agent model). Most of it is already implemented.
