# spec/93 — Dual-Run Mode (Strangler Pattern)

**Branch:** `feature/kafka-distributed-processing`
**Package:** `iped.distributed.dualrun`

---

## Problem

Replacing a battle-tested monolithic forensics pipeline with a new distributed one is risky.
A hard cutover means if the distributed path has a bug (missed items, wrong metadata) the
investigator may not know until after the case is closed.

The strangler-fig / dual-run pattern solves this: run **both** pipelines on the same evidence,
compare their outputs automatically, and only cut over once the comparison says MATCH.

---

## Design

### Matching key: item path

Items in the distributed path carry a UUID that is not present in the monolithic path (which
uses Lucene trackIDs and internal IDs).  The canonical **file-system path** (evidence-relative,
including parent-container hierarchy) is a stable identity that both paths agree on.

Coverage comparison and attribute comparison are both keyed by path.

### `ItemSummary`

```java
record ItemSummary(String uuid, String path, String mediaType, Long lengthBytes)
```

- **Distributed side**: extracted from `DISCOVERED`/`SUBITEM_DISCOVERED` status events.
  `uuid`, `mediaType`, and `lengthBytes` are populated from the enriched `ItemStatusEvent`
  (see §ItemStatusEvent enrichment).
- **Reference side**: supplied by the operator via REST API; `uuid` is null (not meaningful
  on the monolithic side).  `mediaType` and `lengthBytes` are optional enrichment.

### Comparison rules

| Check | Trigger |
|-------|---------|
| Missing from distributed | Path in reference but not in distributed |
| Extra in distributed | Path in distributed but not in reference |
| `mediaType` mismatch | Both sides have non-null value AND values differ |
| `lengthBytes` mismatch | Both sides have non-null value AND values differ |

If only one side has a field (null on the other), it is treated as unknown — no mismatch.
This allows incremental enrichment: submit a reference with paths only, get coverage verdict;
resubmit with full metadata to also check attributes.

### `DualRunVerdict`

- **INCOMPLETE** — at least one side has not finished; report is partial.
- **MATCH** — all three discrepancy lists empty, both sides finished.
- **MISMATCH** — at least one discrepancy exists.

### Discrepancy list cap

Each discrepancy list in `DualRunReport` is capped at 500 entries to keep API responses
bounded.  The true counts (`totalMissing`, `totalExtra`, `totalAttributeMismatches`) are
always the full numbers, even when lists are truncated.

### `ItemStatusEvent` enrichment (non-breaking)

Added `mediaType` (String) and `lengthBytes` (Long) fields.  `base()` factory now populates
them from `KafkaItemMessage`.  Adding fields is non-breaking per the schema evolution policy:
old consumers with `@JsonIgnoreProperties(ignoreUnknown=true)` ignore the new fields.

---

## Flow

```
1. Case starts  →  coordinator creates DualRunSession (if dualRunEnabled)
                   OR operator calls POST /dualrun/{caseId}/start

2. Distributed pipeline runs  →  DISCOVERED events feed DualRunSession.recordEvent()
                               →  CASE_COMPLETED marks distributedComplete=true

3. Monolithic pipeline runs  →  operator calls POST /dualrun/{caseId}/reference
                                body: {"items":[{"path":"/ev/foo.jpg","mediaType":"image/jpeg","lengthBytes":12345},…]}

4. Operator calls GET /dualrun/{caseId}  →  coordinator returns DualRunReport JSON
                                             with verdict, counts, discrepancy lists
```

Step 2 and 3 are independent and can happen in any order.  The report is always available —
it shows INCOMPLETE until both sides are ready.

---

## REST API

### `POST /api/v1/dualrun/{caseId}/start`

Open a dual-run session manually.  Idempotent — safe to call if a session already exists.

**Response 200:**
```json
{"caseId":"ev2025-001","status":"session-open"}
```

---

### `POST /api/v1/dualrun/{caseId}/reference`

Submit the reference (monolithic) item list.  Items must have a non-null `path`.
`mediaType` and `lengthBytes` are optional.

**Request body:**
```json
{
  "items": [
    {"path": "/evidence/image.dd/00001.jpg", "mediaType": "image/jpeg", "lengthBytes": 204800},
    {"path": "/evidence/image.dd/report.pdf", "mediaType": "application/pdf", "lengthBytes": 51200},
    {"path": "/evidence/image.dd/", "mediaType": null, "lengthBytes": null}
  ]
}
```

**Response 200:**
```json
{"caseId":"ev2025-001","referenceItemCount":3}
```

---

### `GET /api/v1/dualrun/{caseId}`

Get the current comparison report.

**Response 200 (MATCH example):**
```json
{
  "caseId": "ev2025-001",
  "verdict": "MATCH",
  "distributedItemCount": 1284,
  "referenceItemCount": 1284,
  "missingFromDistributed": [],
  "totalMissing": 0,
  "extraInDistributed": [],
  "totalExtra": 0,
  "attributeMismatches": [],
  "totalAttributeMismatches": 0,
  "distributedComplete": true,
  "referenceSubmitted": true,
  "generatedAt": "2026-06-14T10:22:00Z"
}
```

**Response 200 (MISMATCH example):**
```json
{
  "caseId": "ev2025-002",
  "verdict": "MISMATCH",
  "distributedItemCount": 1285,
  "referenceItemCount": 1284,
  "missingFromDistributed": [],
  "totalMissing": 0,
  "extraInDistributed": ["/evidence/image.dd/recovered-007.jpg"],
  "totalExtra": 1,
  "attributeMismatches": [],
  "totalAttributeMismatches": 0,
  "distributedComplete": true,
  "referenceSubmitted": true,
  "generatedAt": "2026-06-14T10:24:00Z"
}
```

**Response 404** when no session exists for the case.

---

## Configuration

| Key | Type | Default | Description |
|-----|------|---------|-------------|
| `dualRunEnabled` | boolean | `false` | Auto-open a session for every new case. |

---

## What is NOT in scope

- **Deep content comparison.** Hash values, OCR text, and Lucene field values are out of scope — they require index access. Coverage + MediaType + length is the minimum viable comparison.
- **Automatic cutover.** The operator decides when to cut over; the report is advisory.
- **Reference submission from a second Kafka topic.** The current design accepts reference items via REST; a future follow-up could replay a second status topic.
- **Historical comparison.** Sessions are in-memory; a coordinator restart clears them.

---

## Tests (`DualRunComparatorTest`, 25 tests)

- `ItemSummary` construction from event and from reference factory
- INCOMPLETE: distributed not done, reference not submitted, both not done
- MATCH: identical sets, empty-both-sides edge case
- MISMATCH coverage: missing from distributed, extra in distributed, both
- MISMATCH attributes: mediaType diff, lengthBytes diff, null-on-one-side is NOT a mismatch
- `DualRunSession`: accumulates DISCOVERED, ignores non-discovery events, marks CASE_COMPLETED
- `DualRunSession` full match flow end-to-end
- `DualRunManager`: session creation, event routing, null report for unknown case, submitReference contract
- Summary text for all three verdicts
