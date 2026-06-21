# ISSUE-193: Bookmark write tools gated behind BOOKMARKS capability

- Status: done
- Roadmap: [iped-mcp-ROADMAP.md](../roadmaps/iped-mcp-ROADMAP.md)
- Roadmap section: Phase 3 — Write and job tools
- Owner: unassigned
- Created: 2026-06-21
- Updated: 2026-06-21

## Summary

Gate bookmark mutation tools (create, delete, rename, add_items, remove_items) behind the `BOOKMARKS` capability while keeping read tools (list, items) always available.

## Problem

Bookmark mutation should not be possible unless the operator has explicitly granted write capability, while read access to bookmarks should remain unrestricted.

## Acceptance criteria

- [x] `create`, `delete`, `rename`, `add_items`, `remove_items` bookmark tools gated behind `BOOKMARKS` capability.
- [x] `list`, `items` read tools always available regardless of capability grants.

## Updates

### 2026-06-21
- Issue created during roadmap consolidation, extracted from `iped-mcp-ROADMAP.md`, status set to `done` based on the original `[x]` marker.
