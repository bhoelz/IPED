# Architecture Governance

## 1. Team Structure

### 1.1 Core Platform Team (monorepo IPED)
- Owns `iped-engine`, `iped-tasks`, `iped-webapi`.
- Publishes versioned core artifacts.
- Maintains compatibility for internal Java APIs and public OpenAPI.

### 1.2 Distributed Runtime Team (`iped-distributed-runtime`)
- Owns orchestrator, router, worker base services, observability, and deployment assets.
- Integrates polyglot consumers (Java, Python, Go, Rust).
- Consumes `iped-engine`/task artifacts as binary dependencies.

### 1.3 Contracts Team (`iped-event-contracts`)
- Owns `.proto` contracts and multi-language SDK generation.
- Gatekeeper for event schema breaking changes.
- Maintains schema changelog and compatibility matrix inputs.

### 1.4 Web Experience Team (frontend repository)
- Owns the web UI.
- Integrates exclusively through `iped-webapi` OpenAPI.
- No hard dependency on monorepo internal code.

### 1.5 MCP Integration Team (`iped-mcp-server`)
- Owns the Python MCP server and its tool/skill surface for IPED case interaction.
- Integrates primarily through `iped-webapi` OpenAPI and governance-approved API clients.
- Maintains MCP tool compatibility across supported backend API versions.

### 1.6 Architecture Governance Board
- One representative from each team.
- Approves cross-repository breaking changes.
- Defines deprecation windows and migration milestones.

## 2. Versioning Policy (SemVer)

### 2.1 Core repositories
- `iped-engine`, `iped-webapi`, `iped-tasks` use `MAJOR.MINOR.PATCH`.
- `MAJOR`: breaking public API/contract changes.
- `MINOR`: backward-compatible feature additions.
- `PATCH`: backward-compatible fixes.

### 2.2 Event contracts (`iped-event-contracts`)
- Repository versioned with SemVer.
- Protobuf package namespace versioned (`iped.events.v1`, `iped.events.v2` for breaking generation).
- Breaking schema change requires `MAJOR` bump.

### 2.3 Distributed runtime
- `iped-distributed-runtime` has independent SemVer.
- Each release declares supported version ranges for `iped-engine`, `iped-tasks`, and `iped-event-contracts`.

### 2.4 Frontend
- Frontend repository has independent SemVer.
- Each release declares supported `iped-webapi` OpenAPI ranges.

### 2.5 MCP server
- `iped-mcp-server` has independent SemVer.
- Each release declares supported `iped-webapi` OpenAPI ranges.
- MCP tool contract changes that affect clients must follow SemVer compatibility guarantees.

## 3. Compatibility Policy

### 3.1 N/N-1 support
- Runtime must support current (`N`) and previous (`N-1`) versions of `iped-engine` and `iped-event-contracts`.
- Frontend must support current (`N`) and previous (`N-1`) `iped-webapi` major/minor line during deprecation window.
- MCP server must support current (`N`) and previous (`N-1`) `iped-webapi` major/minor line during deprecation window.

### 3.2 Protobuf compatibility rules
Allowed without breaking release:
- Add new optional fields.
- Add new messages.
- Add enum values.

Not allowed without major bump:
- Remove or renumber field tags.
- Change field types incompatibly.
- Reuse removed field tags/names.

Required when removing fields:
- Declare removed tags/names as `reserved`.

### 3.3 OpenAPI compatibility rules
Allowed in MINOR/PATCH:
- Add optional fields.
- Add endpoints.

Not allowed in MINOR/PATCH:
- Remove endpoints.
- Remove fields.
- Make optional fields required.

Breaking changes:
- Only in MAJOR, with API versioning strategy (example: `/api/v2`) and migration period.

### 3.4 Task/plugin compatibility
- Task contract must be versioned.
- Workers must validate supported `task_version` before processing.
- Reprocessing should be triggered when task version materially changes output semantics.

### 3.5 Deprecation window
- Minimum notice: 90 days or 2 minor releases (whichever is longer).
- Removal requires usage telemetry review and governance board approval.

## 4. Release and Quality Gates

### 4.1 Contract-first gates
- Any `.proto` or OpenAPI change must pass automated compatibility checks.

### 4.2 Consumer-driven contract tests
- Runtime and frontend maintain contract tests against target OpenAPI and event contracts.
- MCP server maintains contract tests against supported `iped-webapi` OpenAPI ranges.

### 4.3 Release train
- Recommended monthly coordinated release train for cross-repo integration.
- Critical hotfixes can be released as PATCH out-of-band.

### 4.4 Supply chain controls
- Publish SBOM per release.
- Pin dependency versions in deployment manifests.

## 5. Initial Compatibility Matrix

Baseline date: 2026-05-13

| Runtime (`iped-distributed-runtime`) | Engine (`iped-engine`) | Tasks (`iped-tasks`) | Event Contracts (`iped-event-contracts`) | WebAPI (`iped-webapi`) | Frontend | MCP Server (`iped-mcp-server`) | Status | Notes |
|---|---|---|---|---|---|---|---|---|
| 0.1.x | 5.0.x | 5.0.x | 1.0.x (`iped.events.v1`) | 5.0.x (`/api/v1`) | 0.1.x | 0.1.x | Supported | Initial pilot baseline |
| 0.2.x | 5.0.x, 5.1.x | 5.0.x, 5.1.x | 1.0.x, 1.1.x (`iped.events.v1`) | 5.0.x, 5.1.x (`/api/v1`) | 0.2.x | 0.2.x | Supported | N/N-1 support enforced |
| 0.3.x | 5.1.x, 5.2.x | 5.1.x, 5.2.x | 1.1.x, 1.2.x (`iped.events.v1`) | 5.1.x, 5.2.x (`/api/v1`) | 0.3.x | 0.3.x | Planned | Target after first production hardening |

## 6. RACI

| Domain | Responsible | Accountable | Consulted | Informed |
|---|---|---|---|---|
| Protobuf event contracts | Contracts Team | Architecture Board | Runtime Team, Core Platform Team | Web Experience Team, MCP Integration Team |
| OpenAPI (`iped-webapi`) | Core Platform Team | Core Platform Team | Web Experience Team, Runtime Team, MCP Integration Team | Contracts Team |
| MCP tool/API adapter compatibility | MCP Integration Team | Architecture Board | Core Platform Team, Web Experience Team | Runtime Team, Contracts Team |
| Compatibility matrix maintenance | Distributed Runtime Team | Architecture Board | Core Platform Team, Contracts Team, Web Experience Team, MCP Integration Team | All teams |
| Breaking change approval | Requesting Team | Architecture Board | All affected teams | All teams |

## 7. Change Control Process

1. Open an architecture change proposal (ACP) in the owning repository.
2. Tag affected teams and provide compatibility impact assessment.
3. Run automated compatibility checks and attach results.
4. Obtain governance approval for any breaking change.
5. Publish migration guide and deprecation timeline.
6. Update this matrix before release.
