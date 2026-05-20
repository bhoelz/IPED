# Phase 2 - Web UI Delivery Backlog

## Objective
Turn `ROADMAP_WEB_UI.md` into an execution backlog that can be split into epics, stories, and implementation tasks across frontend, `iped-webapi`, and integration layers.

## Sources
- `ROADMAP_WEB_UI.md`
- `specs/82-phase-0-swing-inventory.md`
- `specs/83-phase-0-viewers-api-web-mapping.md`
- `specs/84-phase-1-openapi-initial.yaml`
- `specs/85-phase-1-events-schema.md`
- `specs/86-phase-1-implementation-backlog.md`

## Planning Assumptions
- MVP target remains browser-first analysis for core workflows.
- `iped-engine` remains the source of truth for case, search, and viewer semantics.
- OpenAPI and event schemas are the integration contract between backend and frontend.
- Native-only viewer gaps may be deferred behind explicit fallback behavior.

## Delivery Rules
- Each story must map to at least one analyst-visible capability or one blocking platform dependency.
- No frontend feature is considered done without matching backend contract coverage and automated tests.
- No backend endpoint is considered done for web UI purposes without a consumer path in the frontend or a mocked contract test proving intended usage.
- Capability rollout should follow the critical analyst journey before advanced parity work.

## Milestone View

### M1 - Executable Skeleton
- Outcome: open case, create session, search, view results, open basic viewer.

### M2 - MVP Analyst Flow
- Outcome: execute core analysis workflows without daily dependency on Swing for basic tasks.

### M3 - Priority Parity
- Outcome: migrate most recurring analyst operations from Swing to web.

### M4 - Rollout Hardening
- Outcome: controlled production adoption with observability, E2E coverage, and rollback paths.

## Epic Backlog

### EPIC-WEB-01 - Frontend Foundation
Goal:
- Establish the web application shell, engineering conventions, and delivery pipeline.

Stories:
- WEB-001: Initialize Angular workspace and baseline application shell.
  - Deliverables: routing, app frame, shared layout primitives, environment config.
  - Acceptance: local build, production build, lint, and unit test commands run in CI.
- WEB-002: Define frontend folder structure and state domains.
  - Deliverables: domain modules for `session`, `search`, `results`, `viewer`, `filters`, `bookmarks`, `jobs`, `layout`.
  - Acceptance: coding pattern documented and used by at least one implemented flow.
- WEB-003: Integrate generated API client from OpenAPI.
  - Deliverables: generation script, typed client package, regeneration workflow.
  - Acceptance: no handwritten request DTOs for covered endpoints.
- WEB-004: Add global error, loading, and notification infrastructure.
  - Deliverables: HTTP interceptors, correlation ID propagation, user-facing error surface.
  - Acceptance: API failures display structured feedback and are traceable in logs.

Dependencies:
- Stable initial OpenAPI snapshot.

### EPIC-WEB-02 - Session and Case Bootstrap
Goal:
- Allow the UI to open a case context and establish a valid analysis session.

Stories:
- WEB-010: Implement backend session bootstrap endpoints from phase 1 backlog.
  - Deliverables: `POST /cases/{caseId}/sessions`, `GET /cases/{caseId}/metadata`.
  - Acceptance: session creation returns stable identity payload with timestamp and case scope.
- WEB-011: Implement frontend case landing and session start flow.
  - Deliverables: case bootstrap route, session bootstrap service, initial workspace state.
  - Acceptance: user can load a case and enter the analysis shell without manual refresh.
- WEB-012: Add session recovery and invalid-session handling.
  - Deliverables: expired/invalid session behavior, retry or restart path.
  - Acceptance: stale session does not leave the UI in inconsistent state.

Dependencies:
- EPIC-WEB-01

### EPIC-WEB-03 - Search and Results Core
Goal:
- Deliver the primary investigative loop: query, retrieve results, sort, paginate, and inspect.

Stories:
- WEB-020: Implement backend search creation and results retrieval endpoints.
  - Deliverables: `POST /cases/{caseId}/search`, `GET /cases/{caseId}/search/{searchId}/results`.
  - Acceptance: deterministic pagination and persisted query/sort/filter shape.
- WEB-021: Build search bar and query submission workflow.
  - Deliverables: query input, submit/reset behavior, loading state, persisted current search context.
  - Acceptance: search request lifecycle works end to end from browser to API.
- WEB-022: Build result table with server-side pagination and sorting.
  - Deliverables: result grid, row selection, sort controls, paging controls.
  - Acceptance: table state remains consistent across paging and sort changes.
- WEB-023: Add result selection state model and event handling.
  - Deliverables: current item state, multi-select support, selection synchronization with detail/viewer panes.
  - Acceptance: selection changes update dependent panels without desynchronization.

Dependencies:
- EPIC-WEB-02

### EPIC-WEB-04 - Item Context and Metadata
Goal:
- Expose the selected item's metadata and relationships as first-class analysis context.

Stories:
- WEB-030: Implement item detail and relationship endpoints.
  - Deliverables: `GET /cases/{caseId}/items/{itemId}`, `GET /cases/{caseId}/items/{itemId}/relationships`.
  - Acceptance: item context loads for selected result with structured relationship payload.
- WEB-031: Build metadata panel for selected item.
  - Deliverables: core properties, expandable sections, safe formatting for large fields.
  - Acceptance: analyst can inspect key metadata without leaving the current workflow.
- WEB-032: Build relationships panel.
  - Deliverables: parent, subitems, duplicates, references, referenced-by.
  - Acceptance: analyst can navigate relationship context from the selected item.

Dependencies:
- EPIC-WEB-03

### EPIC-WEB-05 - Viewer Platform and Basic Viewers
Goal:
- Deliver the first production-capable web viewer stack for common evidence formats.

Stories:
- WEB-040: Implement viewer session backend endpoints.
  - Deliverables: `POST /cases/{caseId}/viewer/sessions`, viewer capability payloads.
  - Acceptance: viewer session returns `viewerSessionId`, `viewerId`, and capability contract.
- WEB-041: Implement rendition endpoints for text/html/image/pdf/bytes.
  - Deliverables: item rendition handlers and consistent MIME/URL metadata.
  - Acceptance: UI can request rendition assets for all MVP viewer types.
- WEB-042: Build viewer host abstraction in frontend.
  - Deliverables: MIME routing, capability-aware toolbar, fallback renderer, empty/error states.
  - Acceptance: viewer host can switch renderer without page reload.
- WEB-043: Deliver text and HTML viewers.
  - Deliverables: text rendering, sanitized HTML rendering, search highlight integration where applicable.
  - Acceptance: supported text/HTML items open with stable rendering and no unsafe content execution.
- WEB-044: Deliver image and PDF viewers.
  - Deliverables: zoom basics, load lifecycle, failure handling, byte/rendition fetch integration.
  - Acceptance: supported image/PDF items open reliably under pilot datasets.
- WEB-045: Deliver basic email viewer.
  - Deliverables: structured header/body rendering using available renditions or normalized payload.
  - Acceptance: analyst can inspect basic email content within the main workflow.
- WEB-046: Implement unsupported-format fallback behavior.
  - Deliverables: explicit unsupported message, download/open-external decision hook, telemetry.
  - Acceptance: unsupported items fail predictably and audibly, not silently.

Dependencies:
- EPIC-WEB-03
- EPIC-WEB-04 for contextual selection

### EPIC-WEB-06 - Search Inside Viewer and Hit Navigation
Goal:
- Preserve the investigative workflow of searching inside rendered content and navigating hits.

Stories:
- WEB-050: Implement viewer search and hit navigation endpoints.
  - Deliverables: `POST /cases/{caseId}/viewer/sessions/{viewerSessionId}/search`, `POST /cases/{caseId}/viewer/sessions/{viewerSessionId}/hits:navigate`.
  - Acceptance: backend returns hit counts and current hit updates per contract.
- WEB-051: Implement viewer toolbar and hit navigation controls.
  - Deliverables: search box, next/previous hit actions, hit counter, disabled states.
  - Acceptance: toolbar behavior follows viewer capability flags.
- WEB-052: Consume viewer events in frontend.
  - Deliverables: `viewer.session.opened`, `viewer.hit.updated`, `viewer.search.completed`.
  - Acceptance: frontend hit state stays synchronized with backend session state.

Dependencies:
- EPIC-WEB-05
- Event schema availability from phase 1

### EPIC-WEB-07 - Filters, Facets, and Investigative Navigation
Goal:
- Rebuild the central narrowing and exploration tools from the Swing workflow.

Stories:
- WEB-060: Implement facet endpoint consumption and state model.
  - Deliverables: `POST /cases/{caseId}/search/{searchId}/facets`, facet DTO normalization.
  - Acceptance: selected search can load facet buckets deterministically.
- WEB-061: Build facet/filter panel for MVP fields.
  - Deliverables: selectable buckets, clear/apply behavior, active filter summary.
  - Acceptance: applying filters refreshes result set while preserving session state.
- WEB-062: Build categories/bookmarks/filter navigation placeholders for parity path.
  - Deliverables: incremental panels with feature-flagged expansion.
  - Acceptance: shell layout supports future parity panels without rework.

Dependencies:
- EPIC-WEB-03

### EPIC-WEB-08 - Selection, Checked Items, and Bookmarks
Goal:
- Preserve the analyst's ability to mark evidence and maintain working sets.

Stories:
- WEB-070: Model checked item state and bulk-selection interactions.
  - Deliverables: UI state for checked items, table integration, basic batch actions.
  - Acceptance: checked state remains stable across pagination and viewer navigation.
- WEB-071: Implement bookmark CRUD backend contract or adapter path.
  - Deliverables: API contract confirmation and service implementation if not already available.
  - Acceptance: bookmark actions are case-scoped and auditable.
- WEB-072: Build bookmark panel and essential actions.
  - Deliverables: create, assign, remove, filter-by-bookmark basics.
  - Acceptance: analyst can persist and revisit marked evidence sets in web UI.

Dependencies:
- EPIC-WEB-03
- Possible backend contract expansion if bookmark endpoints are not yet formalized

### EPIC-WEB-09 - Jobs and Export Flows
Goal:
- Support asynchronous operations required by the core analysis workflow.

Stories:
- WEB-080: Implement export job endpoints.
  - Deliverables: `POST /jobs/export`, `GET /jobs/{jobId}`.
  - Acceptance: export jobs expose lifecycle states and polling contract.
- WEB-081: Build frontend job center for export tracking.
  - Deliverables: job notifications, status polling, terminal-state feedback.
  - Acceptance: analyst can start an export and track completion from the UI.

Dependencies:
- EPIC-WEB-02
- EPIC-WEB-03 for selected-item/search context

### EPIC-WEB-10 - Layout, Preferences, and Interaction Hardening
Goal:
- Make the UI operationally usable for sustained analyst sessions.

Stories:
- WEB-090: Define persisted layout model for web shell.
  - Deliverables: panel visibility/order/size preferences, storage strategy.
  - Acceptance: preferred layout survives session reload.
- WEB-091: Implement critical keyboard shortcuts and focus behavior.
  - Deliverables: shortlist of high-value shortcuts from Swing inventory.
  - Acceptance: shortcut handling does not conflict with browser defaults in unsupported ways.
- WEB-092: Improve large-result performance.
  - Deliverables: virtualization where needed, deferred loading, expensive-panel throttling.
  - Acceptance: result navigation remains responsive under agreed large-case thresholds.

Dependencies:
- EPIC-WEB-03 through EPIC-WEB-07

### EPIC-WEB-11 - Observability, Security, and Operational Readiness
Goal:
- Ensure the system is diagnosable, auditable, and safe for pilot and rollout.

Stories:
- WEB-100: Add end-to-end correlation and metrics coverage.
  - Deliverables: request IDs, latency metrics, frontend error telemetry, viewer lifecycle metrics.
  - Acceptance: critical flows are traceable across frontend and backend logs/metrics.
- WEB-101: Enforce case/session scoping and error contracts.
  - Deliverables: authorization guards, invalid-scope handling, structured validation responses.
  - Acceptance: cross-case leakage and unscoped access are blocked by tests.
- WEB-102: Create rollout feature flags and pilot controls.
  - Deliverables: capability flags, environment toggles, fallback routing to legacy path when required.
  - Acceptance: pilot can enable or disable risky features without redeploying code.

Dependencies:
- All MVP epics

### EPIC-WEB-12 - Test Automation and Regression Harness
Goal:
- Build confidence that the web UI can evolve without breaking forensic workflows.

Stories:
- WEB-110: Add backend contract tests for all MVP endpoints.
  - Deliverables: request/response shape validation against OpenAPI.
  - Acceptance: CI fails on payload drift.
- WEB-111: Add event schema tests for viewer and session events.
  - Deliverables: schema validation, ordering assumptions per session.
  - Acceptance: CI fails on event drift.
- WEB-112: Add frontend unit tests for state reducers/services.
  - Deliverables: search, selection, viewer, job state coverage.
  - Acceptance: critical UI state transitions are verified automatically.
- WEB-113: Add E2E flows for analyst-critical journeys.
  - Deliverables: open case, search, select result, open viewer, search inside viewer, export job.
  - Acceptance: pilot regression suite runs on representative datasets.

Dependencies:
- Parallel to implementation, but complete before rollout hardening sign-off

## Recommended Release Sequence

### Release Slice A
- EPIC-WEB-01
- EPIC-WEB-02
- EPIC-WEB-03
- Partial EPIC-WEB-04
- Partial EPIC-WEB-05

Expected capability:
- Open case, search, inspect selected item, open first basic viewer.

### Release Slice B
- Complete EPIC-WEB-05
- EPIC-WEB-06
- EPIC-WEB-07
- Partial EPIC-WEB-09

Expected capability:
- Core analyst workflow with filters and in-viewer search.

### Release Slice C
- EPIC-WEB-08
- EPIC-WEB-09
- EPIC-WEB-10

Expected capability:
- Persistent working sets, batch operations, async job handling, operational usability.

### Release Slice D
- EPIC-WEB-11
- EPIC-WEB-12

Expected capability:
- Pilot and rollout readiness.

## Suggested Issue Granularity
- Backend endpoint implementation: 1 issue per endpoint pair or tightly related contract set.
- Frontend feature implementation: 1 issue per end-user slice, not per component.
- Viewer implementation: 1 issue per viewer type after the shared host abstraction is in place.
- Observability/security/test work: separate issues when they block release gating.

## Definition of Done
- Code implemented and reviewed.
- Endpoint or UI behavior matches contract/spec.
- Unit tests added.
- Contract or integration tests added where applicable.
- Logs/metrics/errors are instrumented for the new path.
- Documentation or backlog references updated if scope changed.

## Immediate Next Breakdown
1. Create repository issues for EPIC-WEB-01 through EPIC-WEB-05 first, because they define the critical path to the executable skeleton.
2. Freeze the first generated frontend client from `specs/84-phase-1-openapi-initial.yaml`.
3. Decide which bookmark and layout capabilities are MVP versus parity follow-up.
4. Attach a representative regression dataset to WEB-113 before pilot implementation starts.
