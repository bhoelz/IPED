# ISSUE-098: Audit for datasource-specific types in iped-engine-core

- Status: done
- Roadmap: [iped-engine-core-ROADMAP.md](../roadmaps/iped-engine-core-ROADMAP.md)
- Roadmap section: Phase 1 — Boundary enforcement
- Owner: unassigned
- Created: 2026-06-21
- Updated: 2026-06-21

## Summary

Audit `iped-engine-core` for Sleuthkit/UFED/AD1-specific types, which must be zero per the module's charter.

## Problem

`iped-engine-core` is meant to contain no datasource-specific code. The module needed an explicit audit to confirm `UFEDXMLWrapper` and similar classes don't smuggle in datasource SDK coupling.

## Acceptance criteria

- [x] Audit complete: `UFEDXMLWrapper` confirmed pure XML I/O with no UFED SDK coupling.
- [x] Zero Sleuthkit or AD1 imports confirmed.

## Updates

### 2026-06-21
- Issue created during roadmap consolidation, extracted from `iped-engine-core-ROADMAP.md`, status set to `done`.
