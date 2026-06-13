# spec/92 — Distributed Backpressure

**Branch:** `feature/kafka-distributed-processing`
**Package:** `iped.distributed.resource`

---

## Problem

A Task Agent runs on a host with finite heap and disk.  Under heavy load (large video/image batches, deep archive trees) the JVM heap can grow to the point where the GC cannot keep up, or the work/output volume can fill to the point where the agent's writes fail.  Without a signal back to the coordinator, the agent keeps receiving new work even while it is already overwhelmed — eventually crashing and causing the batch to stall until a human restarts it.

The engine's own `ResourceManager` can detect when a running case needs to pause for memory reasons.  That signal should also contribute to the backpressure level the agent reports.

---

## Goals

1. **Report pressure** — on every heartbeat the agent reports its current resource level (NONE/SOFT/HARD) to the coordinator.
2. **Withhold work** — the coordinator excludes HARD-pressure agents from scheduling (they retain their registered slot count; only the *schedulable view* hides them).
3. **Pause intake** — the agent itself pauses its own Kafka partition consumption while HARD; this is a defence-in-depth mechanism in case the coordinator dispatches before it processes the heartbeat.
4. **Automatic recovery** — when pressure clears, the agent resumes intake and becomes schedulable again without operator intervention.
5. **Never preempt in-flight work** — items already submitted to the executor always finish.

---

## Design

### `PressureLevel`

```
NONE  → healthy; schedule normally
SOFT  → approaching a limit; surfaced for observability, no action taken yet
HARD  → at/over a limit; no new work until pressure clears
```

`SOFT` is intentionally advisory at this stage — a future weighted scheduler can use it to de-prefer pressured agents before they hit HARD.

### `ResourcePressurePolicy` (record, validated)

| Threshold              | Default   | Config key                         |
|------------------------|-----------|------------------------------------|
| heap SOFT ratio        | 75%       | `backpressureHeapSoftRatio`        |
| heap HARD ratio        | 90%       | `backpressureHeapHardRatio`        |
| disk free SOFT (bytes) | 10 GB     | `backpressureDiskSoftFreeBytes`    |
| disk free HARD (bytes) | 5 GB      | `backpressureDiskHardFreeBytes`    |

Invariants: `hardRatio ≥ softRatio`, `diskSoftFree ≥ diskHardFree`.

### `ResourcePressureMonitor`

Composes three independent signals; the **worst wins**:

| Signal                    | SOFT condition                 | HARD condition                  |
|---------------------------|--------------------------------|---------------------------------|
| JVM heap used/max ratio   | `≥ heapSoftRatio`              | `≥ heapHardRatio`               |
| Free disk on work volume  | `≤ diskSoftFreeBytes`          | `≤ diskHardFreeBytes`           |
| `ResourceManager` paused  | —                              | `enforceQuotas() == true`       |

Samplers are injected (inner records `HeapSample`, `DiskSample`) for full testability.
`forAgent(policy, workDir, resourceManager)` factory wires live JVM/disk/engine samplers.
Disk sampler returns `Long.MAX_VALUE` (no pressure) on `IOException` so a missing/unmounted
work volume does not erroneously trigger HARD (the operator sees a log warning instead).

### Agent side (`TaskAgent`)

**Heartbeat loop** (runs every `heartbeatIntervalSeconds`):
1. `pressure = pressureMonitor.sample()`
2. Update `volatile lastPressure` (read by the consumer thread).
3. Log transitions: WARN on HARD entry, INFO on recovery from HARD.
4. Call `coordinator.heartbeat(agentId, freeSlots, currentLoad, pressure.level(), pressure.heapRatio())`.

**Poll loop** (runs on the consumer thread):
```
if !draining:
    if lastPressure.level() == HARD:
        consumer.pause(assignment)          // block new deliveries
    else if consumer.paused().nonEmpty:
        consumer.resume(paused)             // pressure cleared → restart intake
        log INFO "intake resumed"
```

The drain path takes priority: once `draining=true`, partitions are never resumed.
Pausing a partition in Kafka does not cancel records already polled — those items
(already queued in the executor) finish normally.

### Coordinator side

- `AgentRegistry.heartbeat(agentId, freeSlots, currentLoad, pressureLevel, pressureRatio)` — stores both fields on `AgentRegistration`.
- `AgentRegistry.schedulableFreeSlots(taskType)` — sums `freeSlots` for all live agents of that type **except** those with `pressureLevel == HARD`.
- `AgentRegistry.pressuredAgentCount(taskType)` — observable counter of HARD agents.
- `CoordinatorServer.handleHeartbeat` — parses `pressureLevel`/`pressureRatio` from the JSON body; falls back to `NONE` on missing/invalid values.

---

## Recovery semantics

| Transition        | Agent intake | Coordinator scheduling |
|-------------------|--------------|------------------------|
| NONE → SOFT       | continues    | continues              |
| SOFT → HARD       | paused       | withheld               |
| HARD → SOFT/NONE  | resumed      | restored               |

Recovery is fully automatic via the heartbeat cycle.  No operator action required.

---

## What is NOT in scope

- **Pre-emptive eviction of in-flight items.** Items already executing on the agent are never cancelled.
- **SOFT-based weighted scheduling.** The scheduler currently treats SOFT and NONE identically for slot counting.  A future pass can de-prefer SOFT agents.
- **Cross-agent rebalancing.** When an agent goes HARD, the coordinator simply stops scheduling to it.  Existing assigned work units are not migrated; they finish on the HARD agent.
- **Disk pressure on the coordinator.** The coordinator writes only `cases.json` (tiny); disk pressure is an agent concern.

---

## Tests (`ResourcePressureTest`, 25 tests)

- Policy record validation (heap/disk invariants)
- Heap threshold: NONE below soft, SOFT at/above soft, HARD at/above hard
- Disk threshold: same three cases
- Engine pressure: HARD even when heap/disk are healthy
- Combined signals: worst-wins combinations
- `PressureLevel` helpers: `atLeast()`, `max()`
- `AgentRegistry` integration: HARD agent excluded from `schedulableFreeSlots`, SOFT agent included, mixed pool
- `ResourcePressure.accepting()` and `none()` snapshot
