# iped-distributed — Evolution Roadmap

> Module purpose: Kafka-based distributed processing — coordinator, processing agents,
> status reporting (`iped.distributed.{agent,config,coordinator,kafka,status}`).
> Implements workstream 4 of the root `ROADMAP.md`.
> Status legend: `[ ]` planned · `[~]` in progress · `[x]` done

## Current state (2026-06)
- Active development branch (`feature/kafka-distributed-processing`).
- [x] Coordinator/agent split with Kafka topics for work distribution.
- [x] Hardening pass: retry with backoff, status consumer, timeout/completion detection.
- [x] Docker compose stack with runner dashboard and retry settings.
- Surfaced on the runner dashboard via the `iped.status` topic.

## Phase 1 — Correctness guarantees
- [ ] Idempotent processors: re-delivered work units must be detected and skipped (or be
      naturally idempotent); document the dedup key per message type.
- [ ] Dead-letter queue with operator tooling: inspect, requeue, or discard DLQ entries.
- [ ] Exactly-once-effect validation: kill agents mid-segment and verify the merged case
      matches a monolithic run (golden-dataset comparison).
- [ ] Poison-message handling: a malformed evidence segment must not stall the partition.

## Phase 2 — Operability
- [ ] Consumer-lag, throughput, and per-stage error metrics exported (Prometheus format,
      per root roadmap's observability NFRs).
- [ ] Structured status events with a versioned schema (`specs/85-phase-1-events-schema.md`
      alignment); schema evolution policy for the `iped.status` topic.
- [ ] Graceful agent drain (finish current segment, commit, exit) for rolling restarts.
- [ ] Coordinator failover story: document and test what happens when the coordinator
      dies mid-case (rebalance vs manual restart).

## Phase 3 — Scale and scheduling
- [ ] Work-unit sizing strategy: adaptive segment sizes based on evidence type/size
      instead of fixed splits.
- [ ] Multi-case scheduling across the agent pool with priorities (urgent cases preempt
      queue order, not running segments).
- [ ] Backpressure to the coordinator when agent storage/memory nears limits, integrated
      with the engine's ResourceManager.

## Phase 4 — Production rollout (5.0)
- [ ] Dual-run mode (strangler pattern): same case processed both paths, results diffed
      automatically — exit criterion for cutover.
- [ ] Security: TLS + auth on Kafka, signed work-unit payloads, audit trail of which
      agent processed which evidence (chain-of-custody requirement).
- [ ] Deployment playbook (topic creation, partitioning, retention) in `DEPLOYMENT-GUIDE.md`.

## Constraints
- Kafka types stay inside this module; the engine sees only lifecycle interfaces.
- Every message schema change needs forward-compatibility (old agents during rolling
  upgrade) or an explicit version gate.

## Progress checks
- Chaos test: N agents, kill K mid-run → case completes, counts match monolithic run.
- DLQ drill: inject poison message → partition keeps flowing, message lands in DLQ.
