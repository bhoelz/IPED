# ISSUE-428: Full reactor mvn verify on JDK 25 as CI gate; Angular island tests in CI

- Status: done
- Roadmap: [iped-webui-server-ROADMAP.md](../roadmaps/iped-webui-server-ROADMAP.md)
- Roadmap section: Phase 3 — Auth, sessions, hardening
- Owner: unassigned
- Created: 2026-06-21
- Updated: 2026-06-21

## Summary

`build-java25-webservices` runs the full reactor `mvn verify --also-make` on Temurin JDK 25 with `-Dwebui.skipFrontend=true`; `build-angular-islands` runs `npm ci`, the TypeScript contract check, and Vitest island specs on Node.js 22.

## Problem

CI needed dedicated jobs covering both the Java backend (on the target JDK) and the Angular island frontend, without forcing the Java job to depend on Node.js.

## Acceptance criteria

- [x] `build-java25-webservices` job runs `mvn verify` on JDK 25 without requiring Node.js.
- [x] `build-angular-islands` job runs `npm ci`, contract check, and Vitest specs on Node.js 22.

## Updates

### 2026-06-21
- Issue created during roadmap consolidation, extracted from `iped-webui-server-ROADMAP.md`, status set to `done` based on the original `[x]` marker.
