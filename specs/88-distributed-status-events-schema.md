# Distributed Status Events — Schema Reference

> **Topic:** `iped.status`  
> **Current schema version:** 1  
> **Related spec:** [85-phase-1-events-schema.md](85-phase-1-events-schema.md) (SSE/WebSocket envelope)

---

## 1. Overview

All task agents and the coordinator publish structured JSON events to the global
`iped.status` Kafka topic.  A single consumer on the coordinator subscribes from
the beginning and fans out to:

- `CaseCompletionMonitor` — detects when all items in a case have been processed
- `DistributedMetrics` — feeds Prometheus counters and gauges

The topic uses `caseId` as the Kafka message key, placing all events for the same
case on the same partition, which gives total ordering per case.

---

## 2. Envelope

Every event is a JSON object with the following top-level fields:

| Field            | Type     | Required | Description |
|------------------|----------|----------|-------------|
| `schemaVersion`  | `int`    | yes      | Integer schema version. `0` means pre-versioned (legacy). Current value is `1`. |
| `type`           | `string` | yes      | Event type — see §3. |
| `caseId`         | `string` | yes      | Case identifier. Also used as the Kafka message key. |
| `itemUuid`       | `string` | no¹      | Stable UUID for the item being processed. |
| `itemPath`       | `string` | no¹      | File path of the evidence item. |
| `taskType`       | `string` | no²      | Fully-qualified task class name, e.g. `iped.engine.task.HashTask`. |
| `pipelineStage`  | `int`    | no²      | Zero-based pipeline stage index. |
| `timestamp`      | `string` | yes      | ISO-8601 instant at event production time, e.g. `2026-06-12T10:00:00Z`. |
| `durationMs`     | `long`   | no³      | Elapsed time in milliseconds since processing started. |
| `errorMessage`   | `string` | no⁴      | Human-readable error description. |
| `errorClass`     | `string` | no⁴      | Fully-qualified exception class name. |
| `details`        | `object` | no       | Arbitrary extra metadata (open-ended, may grow per type). |

¹ Present for all item-level event types; absent for `CASE_COMPLETED`.  
² Present for task-level types (`STARTED`, `COMPLETED`, `SKIPPED`, `ERROR`, `TIMEOUT`); absent for `DISCOVERED`, `SUBITEM_DISCOVERED`, `CASE_COMPLETED`.  
³ Present for `COMPLETED`, `ERROR`, `TIMEOUT`.  
⁴ Present for `ERROR` and `TIMEOUT`.

---

## 3. Event Types

### `DISCOVERED`
A datasource reader has identified a new item.

```json
{
  "schemaVersion": 1,
  "type": "DISCOVERED",
  "caseId": "case-abc",
  "itemUuid": "550e8400-e29b-41d4-a716-446655440000",
  "itemPath": "/evidence/disk.E01",
  "timestamp": "2026-06-12T10:00:00Z"
}
```

### `STARTED`
A task agent has begun processing an item.

```json
{
  "schemaVersion": 1,
  "type": "STARTED",
  "caseId": "case-abc",
  "itemUuid": "550e8400-e29b-41d4-a716-446655440000",
  "taskType": "iped.engine.task.HashTask",
  "pipelineStage": 2,
  "timestamp": "2026-06-12T10:00:01Z"
}
```

### `COMPLETED`
A task agent finished processing successfully.

```json
{
  "schemaVersion": 1,
  "type": "COMPLETED",
  "caseId": "case-abc",
  "itemUuid": "550e8400-e29b-41d4-a716-446655440000",
  "taskType": "iped.engine.task.HashTask",
  "pipelineStage": 2,
  "durationMs": 47,
  "timestamp": "2026-06-12T10:00:01.047Z"
}
```

### `ERROR`
A task agent encountered an unrecoverable error.  The item is sent to the DLQ.

```json
{
  "schemaVersion": 1,
  "type": "ERROR",
  "caseId": "case-abc",
  "itemUuid": "550e8400-e29b-41d4-a716-446655440000",
  "taskType": "iped.engine.task.HashTask",
  "pipelineStage": 2,
  "durationMs": 120,
  "errorMessage": "File not found: /tmp/segment-42.bin",
  "errorClass": "java.io.FileNotFoundException",
  "timestamp": "2026-06-12T10:00:01.120Z"
}
```

### `SKIPPED`
The task decided the item was not applicable (e.g. wrong media type).

```json
{
  "schemaVersion": 1,
  "type": "SKIPPED",
  "caseId": "case-abc",
  "itemUuid": "550e8400-e29b-41d4-a716-446655440000",
  "taskType": "iped.engine.task.VideoThumbTask",
  "pipelineStage": 5,
  "timestamp": "2026-06-12T10:00:02Z"
}
```

### `TIMEOUT`
The coordinator's completion monitor detected that no `COMPLETED` or `ERROR` event
arrived within the configured window after a `STARTED` event.

```json
{
  "schemaVersion": 1,
  "type": "TIMEOUT",
  "caseId": "case-abc",
  "itemUuid": "550e8400-e29b-41d4-a716-446655440000",
  "taskType": "iped.engine.task.HashTask",
  "pipelineStage": 2,
  "durationMs": 120000,
  "errorMessage": "No completion within timeout window (120000ms since STARTED)",
  "timestamp": "2026-06-12T10:02:01Z"
}
```

### `SUBITEM_DISCOVERED`
A task generated a sub-item (e.g. a file inside a ZIP archive) and re-injected it
into the pipeline.  The `itemUuid` is the sub-item's UUID; use `details.parentUuid`
to trace lineage.

```json
{
  "schemaVersion": 1,
  "type": "SUBITEM_DISCOVERED",
  "caseId": "case-abc",
  "itemUuid": "a4c8e7b2-1234-5678-abcd-000000000001",
  "itemPath": "/evidence/disk.E01!/archive.zip!/file.txt",
  "timestamp": "2026-06-12T10:00:03Z"
}
```

### `CASE_COMPLETED`
All items in the case reached the final pipeline stage.

```json
{
  "schemaVersion": 1,
  "type": "CASE_COMPLETED",
  "caseId": "case-abc",
  "timestamp": "2026-06-12T10:30:00Z"
}
```

---

## 4. Schema Evolution Policy

### 4.1 Non-breaking changes (safe, no version bump)

- **Adding a new optional field** — existing consumers ignore it via
  `@JsonIgnoreProperties(ignoreUnknown = true)`.
- **Adding a new enum value to `type`** — consumers that don't recognise it will
  fall through the `switch`/`default` and skip it.

For these changes: document the new field/value, update JSON examples in this file,
no `SCHEMA_VERSION` bump required.

### 4.2 Breaking changes (require `SCHEMA_VERSION` bump)

- **Removing or renaming a field** — consumers expecting the old name will receive
  `null`.
- **Changing the semantics of a field** (e.g. unit change, type change).
- **Removing an existing enum value** from `type`.

For breaking changes:
1. Bump `ItemStatusEvent.SCHEMA_VERSION` (e.g. 1 → 2).
2. Update all factory methods to produce the new schema.
3. Update `ItemStatusConsumer.validateSchemaVersion()` if the old version requires
   special handling.
4. Add a migration note to the **Version History** section below.
5. Update this file's examples.

### 4.3 Consumer forward-compatibility guarantee

`ItemStatusConsumer` always processes events it receives, even if `schemaVersion`
exceeds the constant it was compiled with.  Unknown fields are dropped silently and
a `WARN` is logged so operators know an upgrade may be rolling out.  This prevents
a coordinator running old code from blocking on events from a newly-deployed agent.

### 4.4 Consumer backward-compatibility guarantee

`schemaVersion == 0` (events produced before versioning was introduced) is treated
identically to `schemaVersion == 1` at the `DEBUG` log level.  All v1 fields were
already present in the pre-versioned wire format.

---

## 5. Topic Configuration

| Parameter                | Recommended value |
|--------------------------|-------------------|
| Partitions               | 12 (tune for peak case concurrency) |
| Replication factor       | 3 |
| Retention (time)         | 7 days |
| Retention (bytes)        | unlimited |
| Cleanup policy           | `delete` |
| `max.message.bytes`      | 1 MB (events are small; increase only if `details` grows) |
| Consumer group strategy  | `fromBeginning=true` + unique group per coordinator restart to rebuild full state |

---

## 6. Version History

| Schema version | Date       | Summary |
|---------------|------------|---------|
| 0             | 2026-05    | Pre-versioned wire format.  All v1 fields present; no `schemaVersion` field. |
| 1             | 2026-06-13 | Added `schemaVersion` field to all events.  No other structural change. |
