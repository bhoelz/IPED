# ISSUE-012: Encode the Lucene-index-as-source-of-truth rule in tests/docs

- Status: done
- Roadmap: [iped-additional-index-ROADMAP.md](../roadmaps/iped-additional-index-ROADMAP.md)
- Roadmap section: Phase 1 — Connector contract
- Owner: unassigned
- Created: 2026-06-21
- Updated: 2026-07-16

## Summary

The main Lucene case index must remain the authoritative source of truth; additional stores (graph, vector, time-series) are strictly additive and must always reference original evidence item IDs. This rule needs to be encoded both in documentation and in automated tests.

## Problem

Without an enforced rule, additional-store connectors could drift into becoming independent sources of truth or lose traceability to the original evidence items, undermining forensic integrity.

## Acceptance criteria

- [ ] Documentation states the Lucene index is the source of truth and additional stores are additive only.
- [ ] A test verifies additional-store entries always reference a valid original evidence item ID.

## Updates

### 2026-06-21
- Issue created during roadmap consolidation, extracted from `iped-additional-index-ROADMAP.md`, status set to `planned` based on the original `[ ]` marker.

### 2026-07-16
- Added a regression test proving additional-index records remain traceable to the original evidence item ID and do not cross-match another item.
