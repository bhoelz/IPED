# Phase 1 - Event Schema Baseline (SSE/WebSocket)

## Envelope (v1)
All domain events must use this envelope:

```json
{
  "eventType": "viewer.hit.updated",
  "version": "1.0.0",
  "timestamp": "2026-05-12T15:00:00Z",
  "caseId": "case-123",
  "sessionId": "sess-789",
  "correlationId": "req-456",
  "payload": {}
}
```

## Required envelope fields
- `eventType`: stable identifier (`domain.entity.action`).
- `version`: semantic version of payload contract.
- `timestamp`: RFC3339 UTC datetime.
- `caseId`: case context.
- `sessionId`: UI/API session context.
- `correlationId`: request or workflow trace identifier.
- `payload`: event-specific content.

## Event catalog (Phase 1)

### `viewer.session.opened`
Payload:
```json
{
  "viewerSessionId": "vs-001",
  "viewerId": "multi-viewer",
  "itemId": "item-abc",
  "mimeType": "text/html"
}
```

### `viewer.session.closed`
Payload:
```json
{
  "viewerSessionId": "vs-001",
  "reason": "user_close"
}
```

### `viewer.capabilities.changed`
Payload:
```json
{
  "viewerSessionId": "vs-001",
  "capabilities": {
    "search": true,
    "hitsMode": "external",
    "toolbar": { "supported": true, "visible": true }
  }
}
```

### `viewer.search.completed`
Payload:
```json
{
  "viewerSessionId": "vs-001",
  "term": "hash",
  "totalHits": 12,
  "currentHit": 1
}
```

### `viewer.hit.updated`
Maps legacy `HitsUpdater.updateHits(currHit, totHits)`.
Payload:
```json
{
  "viewerSessionId": "vs-001",
  "currentHit": 2,
  "totalHits": 12
}
```

### `results.selection.changed`
Payload:
```json
{
  "searchId": "search-001",
  "selectedItemIds": ["item-1", "item-2"],
  "selectedRowIndexes": [10, 11]
}
```

### `results.checkall.changed`
Payload:
```json
{
  "searchId": "search-001",
  "checked": true
}
```

### `results.sort.changed`
Maps legacy `RowSorterTableDataChange.sortKeys`.
Payload:
```json
{
  "searchId": "search-001",
  "sort": [
    { "field": "date", "direction": "desc" }
  ]
}
```

### `case.data.changed`
Payload:
```json
{
  "changeType": "case_refresh",
  "reason": "update_case_data_action",
  "affectedScopes": ["search", "facets", "viewers"]
}
```

### `layout.changed`
Payload:
```json
{
  "layoutId": "default-horizontal",
  "verticalLayout": false,
  "panes": [
    { "id": "results", "visible": true },
    { "id": "viewers", "visible": true }
  ]
}
```

## Versioning rules
- Additive payload changes:
  - Keep `eventType` and bump `version` minor.
- Breaking payload changes:
  - Create new major version, preserve old event for compatibility window.
- Deprecation:
  - Publish replacement event type and deprecation date in release notes.

## Transport guidance
- SSE endpoint (recommended first): `GET /events?caseId={caseId}&sessionId={sessionId}`
- WebSocket channel (optional second step): `/ws/events`
- Ordering:
  - Guarantee per `sessionId`.
- Replay:
  - Optional cursor/last-event-id support in next phase.
