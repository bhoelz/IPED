# ISSUE-418: Remove SearchStubController, proxy to real v2 search

- Status: done
- Roadmap: [iped-webui-server-ROADMAP.md](../roadmaps/iped-webui-server-ROADMAP.md)
- Roadmap section: Phase 1 — Real data end-to-end
- Owner: unassigned
- Created: 2026-06-21
- Updated: 2026-06-21

## Summary

`SearchStubController` was deleted and replaced by `SearchBffController`, which encodes the query into a stateless Base64 `searchId` token and proxies to `GET /v2/search` on `iped-webapi`, re-shaping the response to the island's two-step contract. A 503 from webapi (no cases open) is relayed cleanly.

## Problem

Search results were served from a stub controller rather than the real engine-backed search API, blocking real usage of the workspace.

## Acceptance criteria

- [x] `SearchStubController` deleted.
- [x] `SearchBffController` proxies `POST /api/cases/{caseId}/search` and `GET …/{searchId}/results` to `GET /v2/search`.
- [x] HTTP 503 from webapi (no cases open) is relayed so the UI can show a "no cases open" state.

## Updates

### 2026-06-21
- Issue created during roadmap consolidation, extracted from `iped-webui-server-ROADMAP.md`, status set to `done` based on the original `[x]` marker.
