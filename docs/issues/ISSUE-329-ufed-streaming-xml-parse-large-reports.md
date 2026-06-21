# ISSUE-329: Streaming XML parse for very large UFDR reports

- Status: planned
- Roadmap: [iped-ufed-ROADMAP.md](../roadmaps/iped-ufed-ROADMAP.md)
- Roadmap section: Phase 2 — Format coverage
- Owner: unassigned
- Created: 2026-06-21
- Updated: 2026-06-21

## Summary

Large UFDR reports should be parsed with a streaming XML approach to avoid DOM-loading multi-gigabyte files into memory; peak memory usage should be measured on a large real report to validate the approach.

## Problem

DOM-based XML parsing of very large UFDR reports likely causes excessive memory usage or failures on multi-GB exports.

## Acceptance criteria

- [ ] UFDR XML parsing uses a streaming approach instead of full DOM loading.
- [ ] Peak memory usage is measured against a large real-world UFDR report and recorded.

## Updates

### 2026-06-21
- Issue created during roadmap consolidation, extracted from `iped-ufed-ROADMAP.md`, status set to `planned` based on the original `[ ]` marker.
