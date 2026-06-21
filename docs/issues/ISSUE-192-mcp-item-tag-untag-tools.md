# ISSUE-192: iped_item_tag / iped_item_untag bookmark-backed tagging tools

- Status: done
- Roadmap: [iped-mcp-ROADMAP.md](../roadmaps/iped-mcp-ROADMAP.md)
- Roadmap section: Phase 3 — Write and job tools
- Owner: unassigned
- Created: 2026-06-21
- Updated: 2026-06-21

## Summary

Add `iped_item_tag`/`iped_item_untag` tools backed by the bookmark system, with auto-create semantics so tagging an item with a new tag name creates the bookmark on first use.

## Problem

AI clients triaging evidence need a simple way to tag/untag items without manually managing bookmark lifecycle first.

## Acceptance criteria

- [x] `iped_item_tag`/`iped_item_untag` implemented in `TagTools`.
- [x] Auto-create semantics for new tag names.
- [x] Tools require the `BOOKMARKS` capability.

## Updates

### 2026-06-21
- Issue created during roadmap consolidation, extracted from `iped-mcp-ROADMAP.md`, status set to `done` based on the original `[x]` marker.
