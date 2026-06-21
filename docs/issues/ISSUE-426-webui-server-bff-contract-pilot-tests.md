# ISSUE-426: BFF contract tests and pilot tests

- Status: done
- Roadmap: [iped-webui-server-ROADMAP.md](../roadmaps/iped-webui-server-ROADMAP.md)
- Roadmap section: Phase 3 — Auth, sessions, hardening
- Owner: unassigned
- Created: 2026-06-21
- Updated: 2026-06-21

## Summary

18 tests across `WorkspaceBffContractTest` and `WorkspacePilotTest`, both using `@SpringBootTest(webEnvironment=MOCK)` with `MockMvcBuilders.webAppContextSetup(context).apply(springSecurity())` (since `@AutoConfigureMockMvc` was removed in Spring Boot 4), authenticated via class-level `@WithMockUser`.

## Problem

Once auth was fully enforced, the test setup needed updating for Spring Boot 4's removal of `@AutoConfigureMockMvc`, and contract/pilot coverage needed to run against the now-secured filter chain.

## Acceptance criteria

- [x] `WorkspaceBffContractTest` and `WorkspacePilotTest` (18 tests total) pass against the secured app.
- [x] Tests use `MockMvcBuilders...apply(springSecurity())` and `@WithMockUser`.

## Updates

### 2026-06-21
- Issue created during roadmap consolidation, extracted from `iped-webui-server-ROADMAP.md`, status set to `done` based on the original `[x]` marker.
