# IPED 5.0 Roadmap

## Purpose
IPED 5.0 focuses on platform modernization: a browser-first experience, a companion desktop app for native capabilities, AI-ready integration, and backend decoupling for distributed scale.

## Milestone Scope
Major goals for 5.0:
- Browser-based main UI
- Companion desktop app
- MCP server for AI interaction with cases
- Backend decoupling and distributed processing with Apache Kafka
- Support additional data stores for complementary analysis data (graph, vector, time series)
- Fully documented JavaScript and Python scripting APIs

## Guiding Principles
- Preserve forensic reliability and reproducibility.
- Keep `iped-engine` authoritative for core case/search semantics.
- Deliver incremental user value via phased parity and controlled rollout.
- Prefer explicit contracts between subsystems (API/event/schema-first).

## Workstreams

### 1) Browser-Based Main UI
Objective:
- Replace Swing-first workflow with a production browser UI.

Deliverables:
- Angular 21 + PrimeNG 21 frontend shell.
- Case/session/search/results/filter/viewer navigation flows.
- Viewer framework with MIME routing and fallback handling.
- Role-ready authentication/session model (if deployment requires).

Priority capabilities:
- Search + result table + metadata panel.
- Core trees (categories/bookmarks/filters).
- Initial viewer set (text/html/image/pdf/email-basic).

Deferred/advanced:
- Full graph/timeline parity.
- Long-tail specialized viewers.

### 2) Companion Desktop App
Objective:
- Support native/legacy viewer capabilities not yet feasible in browser.

Deliverables:
- Cross-platform companion app (installer, update channel, logging).
- Secure handoff protocol from web app to native viewer runtime.
- Viewer bridge lifecycle events (open, loading, ready, error, closed).

Initial target viewers:
- LibreOffice-embedded workflows.
- Other native-dependent viewers (as validated by parity tests).

Design constraints:
- Least-privilege execution.
- Signed binaries and strict version compatibility with backend/frontend.
- Clear retirement path per bridged viewer.

### 3) MCP Server for AI Case Interaction
Objective:
- Expose case-safe capabilities to AI clients through an MCP server.

Deliverables:
- MCP tools for query, item retrieval, metadata/facet exploration, and exports.
- Access control and auditability for AI-initiated operations.
- Prompt-safe data shaping and redaction hooks.

Baseline tool surface:
- `case_list`, `case_open`, `search_query`, `search_facets`.
- `item_get`, `item_content_preview`, `item_relationships`.
- `job_create_export`, `job_status`.

Operational requirements:
- Tenant/case isolation.
- Rate limiting and request tracing.
- Immutable audit trail of tool invocations.

### 4) Backend Decoupling + Kafka Distributed Processing
Objective:
- Decompose processing and indexing pipelines into event-driven services.

Deliverables:
- Domain event model for ingestion, parsing, enrichment, indexing, export.
- Kafka topics, consumer groups, retry/DLQ strategy.
- Idempotent processors and replay-safe semantics.
- Observability stack (lag, throughput, errors, SLA dashboards).

Target architecture:
- Control plane APIs (case/job orchestration).
- Data plane workers (parse/enrich/index/render/export).
- Event-driven coordination replacing tight in-process coupling where needed.

Migration strategy:
- Strangler pattern around existing monolith boundaries.
- Dual-run critical paths before cutover.

### 5) Additional Data Stores for Complementary Analysis
Objective:
- Support specialized stores without changing core evidentiary model.

Deliverables:
- Pluggable storage integration layer and connector contracts.
- Graph DB integration for relationship analysis.
- Vector DB integration for semantic/similarity search augmentation.
- Time series DB integration for temporal telemetry/analytics.

Rules:
- Lucene/index remains source of truth for core search unless explicitly superseded.
- Complementary stores are additive and traceable to original evidence IDs.
- Synchronization and consistency policies documented per store.

### 6) JavaScript and Python Scripting APIs
Objective:
- Provide first-class automation and extension APIs with stable documentation.

Deliverables:
- Versioned JS and Python SDKs/wrappers.
- API reference docs, cookbook recipes, and end-to-end examples.
- Sandbox/security model and capability matrix.
- Compatibility policy (semantic versioning and deprecation windows).

Minimum API domains:
- Case/query/result navigation.
- Item content/metadata access.
- Tagging/bookmark/filter operations.
- Export/job orchestration.

Documentation quality bar:
- Runnable examples in CI.
- Cross-language parity table.
- Migration guides for each breaking revision.

## Phased Timeline (5.0)

### Phase A - Foundations
- API contract baselines for web UI, companion, and MCP.
- Kafka platform bootstrap and topic governance.
- Scripting API draft + reference implementation stubs.

Exit criteria:
- Approved interface specs.
- Working end-to-end skeleton (UI -> API -> engine -> response).

### Phase B - MVP Delivery
- Browser UI MVP (search/results/basic viewers).
- Companion app MVP for at least one blocked native viewer.
- MCP server MVP with read-focused toolset.
- Initial Kafka-backed worker path in non-critical flow.

Exit criteria:
- Real-case pilot validation.
- Performance and correctness within agreed thresholds.

### Phase C - Scale and Parity
- Expanded viewer coverage and workflow parity.
- Production-grade Kafka orchestration for major pipelines.
- Graph/vector/time-series connectors in controlled availability.
- Scripting APIs GA docs with cookbook and CI-validated examples.

Exit criteria:
- Operational readiness review passed.
- Security, audit, and rollback plans validated.

### Phase D - Hardening and Release
- Migration tooling and rollout playbook.
- Backward compatibility gates and release candidate program.
- Final 5.0 release notes and deprecation notices.

Exit criteria:
- 5.0 release sign-off.

## Cross-Cutting Non-Functional Requirements
- Security: authN/authZ, data protection, signed artifacts, audit logs.
- Observability: tracing, metrics, logs, business telemetry.
- Performance: large-case pagination, viewer latency, queue throughput.
- Reliability: retry policies, idempotency, disaster recovery.
- Compliance: chain-of-custody and reproducibility safeguards.

## Dependencies and Risks
Key dependencies:
- Stable backend contracts before broad frontend implementation.
- Kafka platform SRE readiness.
- Native viewer packaging/signing pipeline.

Top risks:
- Viewer parity delays for specialized formats.
- Event-model drift between legacy and distributed paths.
- Data consistency across complementary stores.

Mitigations:
- Contract tests and golden datasets.
- Dual-run plus replay validation for critical workflows.
- Feature flags and staged rollouts.

## Acceptance Criteria for 5.0
- Browser UI is the primary supported interface for core analyst workflows.
- Companion app handles validated native-only viewer cases with audited handoff.
- MCP server supports secure AI-assisted case interaction in production.
- Core processing supports Kafka-based distributed execution for target pipelines.
- At least one graph, one vector, and one time-series store integration is production-usable.
- JS and Python scripting APIs are fully documented, versioned, and CI-validated.
