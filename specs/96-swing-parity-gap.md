# Swing → Browser Parity Gap (Phase 0 Inventory Output)

> Source of Swing feature inventory: `specs/82-phase-0-swing-inventory.md`
> This document tracks the delta — what the Swing UI has that the browser UI does not yet
> have, and the browser equivalent (or migration target) for each gap.
>
> Status legend: `[ ]` gap · `[~]` in progress · `[x]` browser equivalent shipped

## A. Search and Filtering

| Swing Feature | Status | Browser Equivalent / Notes |
|---|---|---|
| Query combo with history | `[ ]` | Input at top of workspace page; history not yet persisted |
| Dedup toggle | `[x]` | Chip toggle in topbar (C-01); dispatches filter to results-grid |
| Category filter tree | `[x]` | Sidebar "Categories" tab (HTMX fragment from `GET /v2/sources/{src}/items/categories`) |
| Bookmark filter | `[x]` | Sidebar "Bookmarks" tab; `GET /v2/bookmarks` |
| Date-range filter (metadata panel) | `[x]` | Sidebar metadata panel date-from/to inputs (C-05) |
| Size filter (metadata panel) | `[x]` | Sidebar metadata panel size slider (C-05) |
| AI/classifier filter (similarity) | `[~]` | Sidebar AI filter section (C-06); similar-image-search event wired to gallery island |
| Similar images (current item) | `[~]` | `similar-image-search` event from gallery island → hash query; no face similarity yet |
| Similar documents (threshold) | `[ ]` | Not yet in browser UI; requires a dedicated endpoint |
| Export indexed terms | `[ ]` | No equivalent; low priority |

## B. Result Table and Selection

| Swing Feature | Status | Browser Equivalent / Notes |
|---|---|---|
| Result table with sortable columns | `[x]` | `<iped-results-grid>` Angular island |
| Gallery view | `[x]` | `<iped-gallery>` island with blur/grayscale toggles |
| Timeline view | `[x]` | `<iped-timeline>` island |
| Graph/link view | `[x]` | `<iped-graph>` island |
| Map/geo view | `[x]` | `<iped-map>` Leaflet island (GeoV2 endpoint) |
| Check/uncheck highlighted | `[ ]` | Keyboard shortcut `SPACE`; no browser equivalent yet |
| Check highlighted + subitems/parent/refs | `[ ]` | `CTRL+R/P/F/D`; no browser equivalent yet |
| Read/unread highlighted | `[ ]` | Not yet exposed in browser UI |
| Pagination | `[x]` | Previous/next page buttons in main panel topbar |
| Column management (pin/order) | `[ ]` | Results-grid uses fixed columns for now |

## C. Viewers

| Swing Feature | Status | Browser Equivalent / Notes |
|---|---|---|
| Text / HTML viewer | `[x]` | `<iped-viewer>` text+html modes |
| Hex viewer | `[x]` | `<iped-hex-viewer>` island (byte-range pagination) |
| Metadata viewer | `[x]` | Info-panel HTMX fragment |
| Image viewer (TIFF/JPEG/PNG…) | `[x]` | `<iped-viewer>` image mode; TIFF→PNG via `?format=png` |
| PDF viewer | `[x]` | `<iped-viewer>` pdf mode (`<embed type=pdf>`) |
| Audio player + transcription | `[x]` | `<iped-viewer>` audio mode; non-native codecs → `?transcode=webm` |
| Video player | `[x]` | `<iped-viewer>` video mode; non-native → `?transcode=webm` |
| Email/MSG viewer | `[x]` | `<iped-viewer>` email mode; CID inline-image rewriting is a follow-up |
| LibreOffice viewer | `[ ]` | Companion app target (iped-viewers Phase 3) |
| CAD viewer (proprietary SDK) | `[ ]` | Companion app target (iped-viewers Phase 3) |
| ReferencedFileViewer (OS shell) | `[ ]` | Companion app target (iped-viewers Phase 3) |
| Search/highlight in viewer | `[~]` | `?highlight=` on TextV2; no next-hit navigation yet |
| Copy viewer image | `[ ]` | Not yet in browser UI |
| Previous / next search hit | `[ ]` | TextV2 returns `<mark>` tags; no keyboard nav in viewer yet |

## D. Bookmarks and Export

| Swing Feature | Status | Browser Equivalent / Notes |
|---|---|---|
| Bookmark CRUD | `[x]` | BookmarksV2 REST + `iped_item_tag/untag` MCP tools |
| Per-item bookmark shortcuts | `[ ]` | Keyboard shortcuts not yet in browser UI |
| Export highlighted/checked to ZIP | `[x]` | `POST /v2/jobs` export type; SSE progress |
| Export tree | `[ ]` | Tree-scoped export not yet in browser UI |
| Copy to CSV | `[ ]` | Not yet in browser UI |
| Create report | `[ ]` | `POST /v2/jobs` report type (placeholder backend); browser UI dialog TBD |

## E. Keyboard Shortcuts

All Swing keyboard shortcuts (see `specs/82-phase-0-swing-inventory.md` §C) are in the
gap. Browser equivalents will be implemented incrementally via `keydown` handlers on
the results-grid and viewer islands — priority order:

1. `SPACE` — check/uncheck highlighted row
2. `CTRL+B` — open bookmarks manager
3. Arrow navigation in gallery
4. Previous/next hit navigation in viewer

## F. Feature Freeze Exceptions

This table tracks deliberate choices to **not** port a Swing feature to the browser:

| Feature | Decision | Reason |
|---|---|---|
| DockingFrames panel layout | Not ported | Browser layout is fixed (CSS grid + drag handles); per-user layout persistence via `localStorage` is sufficient |
| Gallery column resize (DockingFrames) | Not ported | `columns-inc`/`columns-dec` custom events replace it |
| Horizontal/vertical layout presets | Not ported | Single responsive layout |
| UI zoom (`setUIZoom`) | Not ported | Browser text zoom (`CTRL++`) is the equivalent |
| Category/gallery icon size sliders | Not ported | Not ported; `columns-inc/dec` covers gallery |

## G. Migration-Blocking Items (before Swing deprecation)

These gaps must be closed before the Swing analysis UI can be moved to deprecated status:

1. **LibreOffice/CAD/ReferencedFile viewers** — companion app bridge (iped-app Phase 3 / iped-viewers Phase 3)
2. **Check/uncheck via keyboard** — selection mutations in browser UI
3. **Similar-document search** — dedicated API endpoint required
4. **Hit navigation in viewer** — next/prev `<mark>` navigation in `<iped-viewer>`
5. **Report generation dialog** — browser UI for `POST /v2/jobs` report type

Until these are resolved, the Swing UI remains the primary analyst interface for workflows
that depend on them.
