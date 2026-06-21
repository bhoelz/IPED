# ISSUE-125: Version/fingerprint the hash DB for distributed mismatch detection

- Status: planned
- Roadmap: [iped-engine-hashdb-ROADMAP.md](../roadmaps/iped-engine-hashdb-ROADMAP.md)
- Roadmap section: Phase 3 — Distributed processing
- Owner: unassigned
- Created: 2026-06-21
- Updated: 2026-06-21

## Summary

The hash DB needs a version or fingerprint so distributed agents can detect mismatched copies; different hash DB versions across agents would make case results non-reproducible.

## Problem

There is no mechanism today for an agent to verify it is using the same hash DB version/content as its peers, risking silent non-reproducibility across a distributed run.

## Acceptance criteria

- [ ] Hash DB carries a version or content fingerprint.
- [ ] Agents can detect and report a mismatched hash DB version before or during processing.

## Updates

### 2026-06-21
- Issue created during roadmap consolidation, extracted from `iped-engine-hashdb-ROADMAP.md`, status set to `planned` based on the original `[ ]` marker.
