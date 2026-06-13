# Coordinator Failover Story

> **Scope:** what happens to a distributed IPED case when the coordinator process
> dies mid-run, and how it recovers.  Covers both the **manual restart** (single
> supervised coordinator) and the **rebalance / HA** question.

---

## 1. The coordinator is not in the data path

The single most important property: **agents process work by consuming the per-case
stage topics directly.** The coordinator does not relay items. It:

- provisions topics (`startCase`)
- tracks agent registration / heartbeats (advisory; for the dashboard and scheduling)
- watches `iped.status` for timeout and case-completion detection
- serves the REST API, DLQ tooling, and `/metrics`

So when the coordinator dies, **in-flight processing keeps running**: agents continue
polling stage topics, executing tasks, forwarding to the next stage, committing offsets,
and publishing status events. No items are lost and none are reprocessed merely because
the coordinator went away — those guarantees come from Kafka offset commits and
deterministic sub-item UUIDs (see Phase 1), not from the coordinator.

What is lost during a coordinator outage is *observability and automatic detection*:
no new timeout events are emitted, no `CASE_COMPLETED` is announced, and the dashboard
goes stale until the coordinator returns.

---

## 2. State inventory — what survives a crash

| State | Holder | Durable? | Recovery on restart |
|-------|--------|----------|---------------------|
| Work queues (stage topics) | Kafka | **Yes** | N/A — agents keep consuming |
| Consumer offsets / progress | Kafka (`__consumer_offsets`) | **Yes** | N/A — agents resume where they left off |
| DLQ contents | Kafka (`*.dlq` topics) | **Yes** | N/A |
| Full status event history | Kafka (`iped.status`) | **Yes** (topic retention) | Replayed on restart |
| **Case definitions** (caseId, ordered task list) | `CaseLifecycleManager` (in-memory) | **Only if `coordinatorStateDir` set** | Loaded from `cases.json` |
| **Completion progress** (discovered / completed / in-flight counts) | `CaseCompletionMonitor` (in-memory) | No | Rebuilt by replaying `iped.status` |
| Agent registry | `AgentRegistry` (in-memory) | No | Self-heals via heartbeats within `agentExpirySeconds` |
| Metrics counters | `DistributedMetrics` (in-memory) | No | Rebuilt from live events (Prometheus counter-reset is expected) |

### Why case definitions need disk

Status events carry a per-item `pipelineStage` number but **not** the authoritative
ordered task list. The number of stages and which task owns each stage cannot be
reconstructed reliably from the event stream alone. Therefore the ordered task list is
the one piece of coordinator state persisted to disk:

- `CaseLifecycleManager(topicProvisioner, stateDir)` writes `cases.json` (atomic
  replace) on every `startCase` and `completeCase`.
- On construction it reloads the file and rebuilds the task-order and stage maps.
- Set the directory via `coordinatorStateDir` in `DistributedConfig.toml` (or the
  `COORDINATOR_STATE_DIR` deployment env). Point it at a mounted volume that survives
  container restarts. Leave it blank for the legacy in-memory-only behaviour.

### Why completion progress can be rebuilt from Kafka

`iped.status` is an append-only event log with all `DISCOVERED`, `STARTED`,
`COMPLETED`, `ERROR`, `SKIPPED`, and `CASE_COMPLETED` events. Replaying it from the
beginning reconstructs the same counters the pre-crash coordinator held:

- `CaseCompletionMonitor.recover(history)` (or `recoverSingle` per record) replays
  events with **side effects suppressed** — it does not re-publish `CASE_COMPLETED`
  or re-fire timeouts.
- A case that already emitted `CASE_COMPLETED` in the history is recognised as
  complete (idempotent) and not re-announced.
- `StatusTopicReplayer` performs a one-shot bounded read (earliest → captured
  log-end) using a throwaway consumer group, committing nothing.

---

## 3. Restart sequence (manual / supervised — the supported model)

On `CoordinatorServer.start()`:

1. `CaseLifecycleManager` loads `cases.json` → case definitions restored.
2. If any case definitions were recovered, `StatusTopicReplayer` replays `iped.status`
   into `CaseCompletionMonitor.recoverSingle` → discovered / completed / in-flight
   counts and "already completed" flags restored. Best-effort: a missing broker/topic
   just leaves progress empty, to be rebuilt from live events.
3. The live `ItemStatusConsumer` starts (group `iped-coordinator`, from `latest`) and
   resumes timeout + completion detection going forward.
4. Agents re-register via their next heartbeat; `AgentRegistry` repopulates within
   `agentExpirySeconds`.
5. Topic provisioning on any subsequent `startCase` is idempotent (`TopicExistsException`
   is treated as success), so re-issuing a case is harmless.

After step 3 the coordinator is fully caught up: items still in flight at crash time are
re-delivered to agents by Kafka and produce fresh `STARTED`/`COMPLETED` events that the
live consumer picks up, so completion is still detected correctly.

### Recovery caveat — the replay/live boundary

`StatusTopicReplayer` captures end offsets once and reads up to them; the live consumer
then starts from `latest`. Events written *between* the replay's end-offset snapshot and
the live consumer's first poll are rare but possible. They are tolerated because:

- Counters are monotonic and idempotent at the case level; a missed mid-boundary
  `COMPLETED` only delays completion detection until the next equivalent signal, and
- in practice the gap is sub-second and dwarfed by the item timeout window.

For exact-boundary correctness a future iteration can have the live consumer seek to the
replay's end offsets instead of `latest`; this is noted as follow-up, not required for
the current single-coordinator model.

---

## 4. Rebalance / HA (multiple coordinators) — not supported yet

Running two coordinators against the same cluster is **not** currently safe:

- Both would provision topics (idempotent, harmless) **but**
- both would run completion monitors and both would publish `CASE_COMPLETED` and
  `TIMEOUT` events — duplicated detection, duplicated side effects.
- The REST API has no leader election, so agents pointed at a load balancer could
  register against either instance and split the registry view.

**Recommended deployment:** exactly one coordinator, under a process supervisor
(systemd, Kubernetes `Deployment` with `replicas: 1`, Docker `restart: unless-stopped`).
Supervised restart + the recovery sequence in §3 gives effective availability: the only
window of degraded behaviour is the seconds between crash and restart, during which
agents keep working uninterrupted.

### Path to true HA (future work)

1. Leader election (e.g. a Kafka single-partition "coordinator-leader" topic, or
   ZooKeeper/etcd lease). Only the leader runs the completion monitor and timeout sweep.
2. Followers serve read-only REST (registry/metrics) and stand by.
3. On leader loss, a follower wins the lease and runs the §3 recovery sequence before
   taking over detection.

This is tracked under Phase 4 (production rollout) and is out of scope for the current
correctness/operability phases.

---

## 5. Operator runbook

**Coordinator crashed:**
1. Confirm agents are still processing (check agent logs or `iped_distributed_*` metrics
   from a still-scraping Prometheus — agents do not stop).
2. Restart the coordinator (supervisor should do this automatically).
3. On startup, confirm the log lines:
   - `Recovered N case definition(s) from .../cases.json`
   - `Replayed M status event(s) from 'iped.status' for coordinator recovery`
4. Verify `/api/v1/cases` lists the in-progress cases and `/metrics` is serving again.

**Coordinator state directory lost (no `cases.json`):**
- Case definitions cannot be auto-recovered. Re-issue `POST /api/v1/cases/start` with the
  same `caseId` and `orderedTaskNames`. Topic provisioning is idempotent; completion
  progress then rebuilds from the replayed `iped.status` history as usual.

---

## 6. Configuration summary

| Setting | Env var | Default | Purpose |
|---------|---------|---------|---------|
| `coordinatorStateDir` | `COORDINATOR_STATE_DIR` | `""` (disabled) | Durable dir for `cases.json` |
| `agentExpirySeconds` | `AGENT_EXPIRY_SECONDS` | `30` | Registry self-heal window |
| `itemTimeoutSeconds` | `ITEM_TIMEOUT_SECONDS` | `3600` | In-flight timeout detection |
