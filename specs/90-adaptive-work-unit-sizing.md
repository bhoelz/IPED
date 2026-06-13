# Adaptive Work-Unit Sizing

> **Package:** `iped.distributed.workunit`
> **Phase:** 3 — Scale and scheduling

---

## 1. Problem: one work unit per item is wrong at both extremes

The distributed pipeline currently publishes **one Kafka message per item** (see
`KafkaItemProducer`). A forensic case is a mix of extremes:

- **Millions of tiny items** — registry keys, cached thumbnails, log lines, e-mail
  fragments — each a few bytes to a few KB. One message + one poll + one offset commit
  + one status event *per item* means fixed overhead dominates throughput.
- **A handful of huge items** — disk images, VM disks, videos — hundreds of MB to many
  GB. These deserve their own work unit and must never be bundled together, or an agent
  picks up several at once and blows its memory/time budget, skewing load across the pool.

A naïve fix — "N items per work unit" — fails the second case: N large items in one unit
is exactly the imbalance we want to avoid. The size of a work unit has to **adapt to the
content**, not be a fixed count or a fixed byte split.

---

## 2. Solution: greedy packing by weighted size

`AdaptiveWorkUnitPlanner.plan(List<KafkaItemMessage>)` groups items into `WorkUnit`s by
greedily packing against a **weighted byte size**:

```
weightedSize(item) = max(0, item.length) × MediaCostModel.weight(item)
```

The `MediaCostModel` assigns a per-type cost multiplier so that types which are expensive
*per byte* fill a unit faster (smaller batches) and cheap dense types batch larger:

| Type | Weight | Rationale |
|------|--------|-----------|
| video/* | 4.0 | transcode + thumbnail generation |
| disk images (E01, raw, VMDK, …) | 3.5 | volume/partition expansion |
| archives (zip, rar, 7z, tar, …) | 3.0 | expand into many sub-items |
| image/* | 1.5 | thumbnails, possible OCR |
| audio/*, text/*, default | 1.0 | baseline |
| directories, zero-length | 0.1 | negligible work |

The model matches on the normalised media type first, then falls back to the file
**extension** (raw stage-0 items often carry only an extension before type detection).

### Packing rules (`WorkUnitSizingPolicy`)

| Knob | Default | Meaning |
|------|---------|---------|
| `targetUnitBytes` | 64 MB | Soft fill target. Items accumulate until adding the next would exceed this (in weighted bytes), then the unit is flushed. |
| `maxUnitItems` | 256 | Hard cap on items per unit. Stops a flood of tiny/zero-length files (whose weighted bytes never reach the target) from forming an unbounded unit. |
| `oversizedItemBytes` | 128 MB | A single item whose weighted size reaches this is isolated into its own unit, flagged `oversized`. Must be ≥ `targetUnitBytes`. |

Algorithm (per item, in input order):

1. If `weightedSize ≥ oversizedItemBytes`: flush any pending unit, emit this item as its
   own `oversized` unit. Continue.
2. Else if the current unit is non-empty **and** adding the item would exceed
   `targetUnitBytes`, **or** the unit already holds `maxUnitItems`: flush the current unit.
3. Add the item to the current unit.
4. At the end, flush any remaining unit.

A unit's items are always **contiguous in the input**, and units are numbered in flush
order, so the plan is a pure, deterministic function of `(input order, policy, cost model)`.

---

## 3. Why determinism matters

The planner is stateless and deterministic: the same input list always yields identical
units. This preserves the pipeline's existing **exactly-once-effect** guarantee
(`specs` Phase 1): deterministic sub-item UUIDs and idempotent indexing rely on work-unit
identity being stable across reader restarts. A non-deterministic packer (e.g. one that
depended on wall-clock arrival timing) would break re-delivery idempotency.

---

## 4. Worked examples

With defaults (`target = 64 MB`, `maxItems = 256`, `oversized = 128 MB`):

- **1000 × 1 KB text files** → byte target never reached, so the item cap governs:
  `ceil(1000 / 256) = 4` units (256, 256, 256, 232).
- **13 × 10 MB text files** (target raised to fit) → 6 + 6 + 1 by byte target.
- **[1 KB, 200 MB, 1 KB]** → three units: `{1 KB}`, `{200 MB, oversized}`, `{1 KB}` — the
  oversized item flushes the pending small item before isolating, and the trailing small
  item starts a fresh unit.
- **Two 10 MB videos** → 2 units (weighted 40 MB each, two exceed a 60 MB target), whereas
  **two 10 MB text files** → 1 unit (weighted 20 MB total). Same bytes, different batching,
  driven entirely by the cost model.

---

## 5. Configuration

`DistributedConfig.toml` (or deployment env):

| Setting | Env var | Default |
|---------|---------|---------|
| `workUnitTargetBytes` | — | `67108864` (64 MB) |
| `workUnitMaxItems` | — | `256` |
| `workUnitOversizedBytes` | — | `134217728` (128 MB) |

Build a policy with `WorkUnitSizingPolicy.fromConfig(cfg)`.

---

## 6. Integration point and scope

The planner is the **sizing decision**, kept as pure logic so it is unit-testable without a
broker. The intended consumer is the reader/producer side (`KafkaItemProducer`): instead of
`addItem` → one message, a future change can buffer a reader's emitted items, call
`plan(...)`, and dispatch each `WorkUnit` as a compound message (or as a contiguous offset
range) carrying its item list. That wiring — and the corresponding `TaskAgent`-side
unpacking of a multi-item work unit — is the follow-up; the cost model, policy, packing
algorithm, config knobs, and their tests land here.

`WorkUnit` already exposes everything the dispatch and scheduling layers need:
`index`, `itemUuids`, `totalBytes`, `weightedBytes`, `oversized`, `itemCount()`.

---

## 7. Tuning guidance

- **Lots of small files, throughput-bound:** raise `workUnitMaxItems` (e.g. 1024) and/or
  `workUnitTargetBytes` to amortise overhead more aggressively.
- **Memory-constrained agents:** lower `workUnitOversizedBytes` so more items are isolated
  and units stay small.
- **A type behaves worse than its weight suggests** (e.g. deeply nested archives): the
  weight table in `MediaCostModel` is the single place to adjust; raising a type's weight
  shrinks its batches without touching the algorithm.
