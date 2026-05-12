# Phase 1 - Implementation Backlog (Contract-First)

## Goal
Turn the initial API/event contracts into an executable implementation sequence for backend and frontend skeleton work.

## Sprint 1 (critical path)
1. Session and case bootstrap
- `POST /cases/{caseId}/sessions`
- `GET /cases/{caseId}/metadata`
- Done when:
  - Session id is generated and returned with timestamp.
  - Case metadata endpoint returns stable case identity payload.

2. Search baseline
- `POST /cases/{caseId}/search`
- `GET /cases/{caseId}/search/{searchId}/results`
- Done when:
  - Search request persists `query`, `filters`, `sort`, `page`.
  - Results endpoint returns deterministic pagination shape.

3. Viewer session baseline
- `POST /cases/{caseId}/viewer/sessions`
- `POST /cases/{caseId}/viewer/sessions/{viewerSessionId}/search`
- `POST /cases/{caseId}/viewer/sessions/{viewerSessionId}/hits:navigate`
- Done when:
  - Viewer capabilities (`search`, `hitsMode`, `toolbar`) are returned.
  - Hit navigation updates `currentHit` and `totalHits`.

## Sprint 2 (MVP completion path)
1. Item and relationship context
- `GET /cases/{caseId}/items/{itemId}`
- `GET /cases/{caseId}/items/{itemId}/relationships`

2. Renditions
- `GET /cases/{caseId}/items/{itemId}/renditions/text`
- `GET /cases/{caseId}/items/{itemId}/renditions/html`
- `GET /cases/{caseId}/items/{itemId}/renditions/image`
- `GET /cases/{caseId}/items/{itemId}/renditions/pdf`
- `GET /cases/{caseId}/items/{itemId}/bytes`

3. Jobs and export
- `POST /jobs/export`
- `GET /jobs/{jobId}`

4. Facets
- `POST /cases/{caseId}/search/{searchId}/facets`

## Event rollout order
1. `viewer.session.opened`
2. `viewer.hit.updated`
3. `viewer.search.completed`
4. `results.selection.changed`
5. `results.sort.changed`
6. `case.data.changed`
7. `layout.changed`

## Definition of Done (per endpoint)
- Contract:
  - Request/response match `84-phase-1-openapi-initial.yaml`.
- Validation:
  - Invalid payload returns structured `4xx` with field-level errors.
- Observability:
  - Endpoint emits latency/error metrics and includes `correlationId`.
- Security:
  - Case/session scoping enforced.
- Tests:
  - Unit test for handler/service.
  - Contract test for payload shape.
  - One integration test covering happy path.

## Definition of Done (per event)
- Envelope:
  - Includes `eventType`, `version`, `timestamp`, `caseId`, `sessionId`, `correlationId`, `payload`.
- Ordering:
  - Preserved per `sessionId`.
- Contract tests:
  - JSON schema validation for each event payload.

## Test matrix (minimum)
1. Search lifecycle:
- create search -> fetch results -> sort change event.

2. Viewer lifecycle:
- open viewer session -> search term -> next hit -> prev hit.

3. Item/rendition lifecycle:
- fetch item -> open text/html/pdf rendition -> byte range fetch.

4. Export lifecycle:
- create export job -> poll job status -> completion payload.

## Handoff notes for Phase 2
- Keep OpenAPI as source of truth and generate server/client stubs from it.
- Introduce schema registry (or versioned JSON schemas) for event payloads.
- Add replay support (`Last-Event-ID`/cursor) in event stream.
