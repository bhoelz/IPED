# Multi-Case Scheduling with Priorities

> **Package:** `iped.distributed.scheduler`
> **Phase:** 3 — Scale and scheduling

---

## 1. Problem: a shared agent pool, many cases, unequal urgency

Each `TaskAgent` is bound to one case (consumer group `{caseId}.{taskType}`, subscribed to
that case's stage topic). When several cases run concurrently against the same fleet of
agents, free agent capacity must be steered to the right case. A new **urgent** case (an
active investigation with a legal deadline) should jump ahead of routine background cases —
but only for *queued* work. Items already executing must finish; we never kill a running
segment mid-flight (the engine's task pipeline assumes an item runs to completion).

So the scheduling decision is narrow and well-defined: **when an agent slot frees up, which
case's queued work should it pull next?**

---

## 2. Policy: strict priority across classes, round-robin within a class

`CaseScheduler.schedule(cases, freeSlots)` returns a list of `SlotAssignment`s — how many
free slots to direct at each case — under this policy:

1. **Skip empty cases.** A case with `pendingWork == 0` is never assigned slots.
2. **Strict priority across classes.** Higher `CasePriority` is served *completely* before
   any lower class. All URGENT pending work that fits is assigned before any HIGH, then
   NORMAL, then LOW. This is how an urgent case "preempts the queue order."
3. **Round-robin within a class.** Among equal-priority cases, slots are handed out one at a
   time, cycling, so no single case monopolises the pool and equal cases share fairly.
4. **Demand cap.** A case never receives more slots than it has `pendingWork`.
5. **Free slots only — no preemption.** The scheduler distributes *free* capacity. It never
   returns a negative or preemptive assignment; in-flight items are untouched
   ("not running segments"). Re-prioritising changes only which case the *next* freed slot
   pulls from.

`CasePriority`: `URGENT (1000) > HIGH (100) > NORMAL (10) > LOW (1)`. The numeric weight
orders classes; only the ordering matters.

### Determinism

Given the same cases, priorities, pending-work counts, and free-slot count, the plan is
identical every time (cases are ordered by priority then `caseId`; the remainder of an
uneven round-robin split goes to the first case in `caseId` order). Determinism keeps
scheduling decisions reproducible and testable.

### Starvation is intentional

Strict priority means a continuous stream of URGENT work *will* starve LOW/NORMAL cases.
That is the requested semantic — urgency preempts the queue. Operators control it by
raising or lowering a case's priority; there is no hidden aging. (A weighted-fair variant
that guarantees a floor for low-priority cases is a possible future option, noted but not
implemented.)

---

## 3. Worked examples

| Free slots | Cases (priority, pending) | Result |
|-----------:|---------------------------|--------|
| 8 | normal(100), urgent(100) | urgent 8, normal 0 |
| 8 | urgent(3), normal(100) | urgent 3, normal 5 |
| 25 | low(10), normal(10), high(10), urgent(10) | urgent 10, high 10, normal 5, low 0 |
| 8 | a NORMAL(100), b NORMAL(100) | a 4, b 4 (round-robin) |
| 7 | a/b/c NORMAL(100 each) | a 3, b 2, c 2 (remainder to first by caseId) |
| 8 | big NORMAL(100), small NORMAL(1) | small 1, big 7 (demand cap) |

---

## 4. Case priority lifecycle

Priority is part of the case definition (`CaseLifecycleManager.CaseStatus.priority`,
default `NORMAL`) and is **persisted** to `cases.json`, so it survives a coordinator
restart alongside the rest of the case definition (see
`specs/89-coordinator-failover.md`). Legacy `cases.json` written before this field load as
`NORMAL`.

### REST API

```
POST /api/v1/cases/start
  body: { "caseId": "...", "orderedTaskNames": [...], "priority": "URGENT" }   # priority optional, default NORMAL

POST /api/v1/cases/{caseId}/priority
  body: { "priority": "HIGH" }                                                  # re-prioritise a running case
  → 200 { "caseId": "...", "priority": "HIGH" }  |  404 if unknown case
```

`GET /api/v1/cases/{caseId}` includes the current `priority` in the returned status.

---

## 5. How the coordinator feeds the scheduler

The scheduler is invoked **per task type** (agents are typed). For a task type, the
coordinator builds one `SchedulableCase` per active case from:

- **priority** — `CaseStatus.priority` from `CaseLifecycleManager`;
- **pendingWork** — consumer lag on that case's stage topic for the task type, from
  `ConsumerLagProvider` (the `{caseId}.{taskType}` consumer group's lag);
- **inFlight** — informational, from the status-event progress or agent load.

…and the **free slots** for that task type from `AgentRegistry.availability()`. The
returned `SlotAssignment`s say which case each typed slot should pull from next.

This keeps the scheduler a **pure decision function** — fully unit-testable without a
broker — while reusing the lag and registry data the coordinator already collects.

---

## 6. Scope and follow-up

What lands here: the priority model, the scheduling algorithm, case-priority lifecycle +
persistence + REST endpoints, and the test suite.

The **enforcement mechanism** — how a `SlotAssignment` actually redirects an agent to a
different case's topic — is the follow-up, and depends on the agent model evolving from
"one agent bound to one case" to "a typed agent that can be steered across the cases that
need its task type" (e.g. dynamic subscription to `{caseId}.stage.{N}` topics driven by the
coordinator's plan, or a coordinator-assigned work-pull protocol). That is a larger change
to `TaskAgent`'s subscription model and is tracked as a Phase 3 wiring follow-up, mirroring
how the adaptive work-unit *dispatch* (specs/90) was separated from its sizing decision.

---

## 7. Tuning / operations

- **Promote a case mid-run:** `POST /cases/{caseId}/priority {"priority":"URGENT"}`. The
  next freed slots across the fleet shift to it; running items elsewhere finish normally.
- **Demote a noisy background case:** set it to `LOW` so it only consumes slots no
  higher-priority case wants.
- **Equal-priority fairness:** cases at the same class share capacity round-robin; split
  a large case into separate caseIds only if you want it to count as multiple round-robin
  participants.
