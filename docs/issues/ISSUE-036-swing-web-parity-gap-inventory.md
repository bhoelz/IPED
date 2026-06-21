# ISSUE-036: Inventory Swing-only features against the web backlog (parity gap list)

- Status: done
- Roadmap: [iped-app-ROADMAP.md](../roadmaps/iped-app-ROADMAP.md)
- Roadmap section: Phase 2 — Maintenance mode for Swing analysis UI
- Owner: unassigned
- Created: 2026-06-21
- Updated: 2026-06-21

## Summary

Build and maintain a parity gap list comparing Swing-only features against the browser UI backlog, identifying which gaps block Swing deprecation.

## Problem

Without a tracked inventory of Swing-only capabilities, it is hard to know which features still need browser UI equivalents before the Swing app can be deprecated.

## Acceptance criteria

- [x] `specs/96-swing-parity-gap.md` created with tables for Search/Filtering, Result Table, Viewers, Bookmarks/Export, Keyboard Shortcuts, Feature Freeze Exceptions, and Migration-Blocking Items.
- [x] Migration-blocking gaps identified (LibreOffice/CAD/ReferencedFile viewers, check via keyboard, similar-document search, hit navigation, report dialog) and listed as prerequisites before Swing can be deprecated.

## Updates

### 2026-06-21
- Issue created during roadmap consolidation, extracted from `iped-app-ROADMAP.md`, status set to `done` based on the original `[x]` marker.
