# ISSUE-380: Generated Python client from the OpenAPI spec

- Status: done
- Roadmap: [iped-webapi-ROADMAP.md](../roadmaps/iped-webapi-ROADMAP.md)
- Roadmap section: Phase 4 — Platform (5.0)
- Owner: unassigned
- Created: 2026-06-21
- Updated: 2026-06-21

## Summary

Generate a Python client from the OpenAPI spec as part of the normal build, giving scripting/automation users a ready-made SDK without hand-writing HTTP calls.

## Problem

Without a generated client, Python-based automation against `iped-webapi` required hand-rolled HTTP request code, duplicating effort and risking drift from the spec.

## Acceptance criteria

- [x] `openapi-generator-maven-plugin` added to `pom.xml`, targeting `specs/84-phase-1-openapi-initial.yaml`.
- [x] Generates a `python` client to `target/generated-clients/python` with package name `iped_client`.
- [x] Bound to the `generate-sources` phase so it runs as part of a normal `mvn package`.

## Updates

### 2026-06-21
- Issue created during roadmap consolidation, extracted from `iped-webapi-ROADMAP.md`, status set to `done` based on the original `[x]` marker.
