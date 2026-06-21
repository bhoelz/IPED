# IPED Desktop → Web: Main Window Migration Plan

> **Status note (2026-06-21):** verified against the current codebase — sidebar tabs
> (C-02 through C-07), the results grid (C-08), gallery (C-09), timeline (C-10), graph
> (C-11), info-panel tabs (C-12/C-13), viewer tabs (C-14 through C-17), the export dialog
> (C-18), similar-image search wiring (C-19), and layout resize handles (C-20) all have
> matching code in `iped-webui`/`iped-webui-server` already. This plan is kept as the
> detailed per-component design rationale (Swing origin → web approach mapping) rather
> than a live backlog — see [iped-webui-ROADMAP.md](../docs/roadmaps/iped-webui-ROADMAP.md)
> and [iped-webui-server-ROADMAP.md](../docs/roadmaps/iped-webui-server-ROADMAP.md) for
> current, issue-tracked status.

## Architecture recap

```
┌─────────────────────────────────────────────────────────┐
│ Spring Boot (iped-webui-server)  SSR shell              │
│  Rocker templates + HTMX fragments own structural       │
│  regions: topbar, sidebar tabs, info-panel tabs,        │
│  viewer tabs, status bar.                               │
│  ─────────────────────────────────────────────────────  │
│  Angular islands own rich interactive surfaces:         │
│  results grid, gallery, timeline, graph, hex viewer.    │
│  Islands speak JSON to /api/**; the SSR shell never     │
│  patches inside an island.                              │
└─────────────────────────────────────────────────────────┘
```

Boundary rule (already in place): attributes in → island; DOM CustomEvents out → SSR bridges react via `htmx.ajax()` or attribute mutation.

---

## Prerequisite: API v2 search contract

**Blocks:** C-08, C-09, C-10, C-12–C-13  
**What:** Replace `SearchStubController` (which wraps legacy `GET /search?q=`) with a real `POST /cases/{id}/search` → `GET /cases/{id}/search/{searchId}/results` round-trip in `iped-webapi`.  
**Test:** Integration test in `iped-webui-server` hitting the real Jersey app.  
**Note:** This is EPIC-WEB-03; `SearchStubController` must be deleted when it lands.

---

## C-01 · Top Bar

**Swing origin:** `topPanel` – query combo, filter combo, search/options/help/export buttons, filter-chips strip  
**Approach:** SSR shell — already partially exists in `page.rocker.html`

Steps:
1. Add a `/workspace/topbar` HTMX fragment endpoint returning the filter-chips strip (active filters as pill badges).
2. Move the `<header class="topbar">` into its own `topbar.rocker.html` template, parameterised with `caseId` and `List<ActiveFilter>` (name + type, no value).
3. Each chip has `hx-delete="/workspace/filter/{id}" hx-target="#topbar" hx-swap="outerHTML"` to remove a filter.
4. Wire the search `<form>` submit to update the `<iped-results-grid>` `query` attribute (already done via `ipedSearch()` bridge).
5. Add `filterDuplicates` toggle as a checkbox chip in the chips strip.

**Test:** Rocker unit-render test with a fixed `ActiveFilter` list; `WorkspacePilotTest`-style HTTP test confirms chip HTML.

---

## C-02 · Categories Tree

**Swing origin:** `categoriesTabDock` → `JTree` with `CategoryTreeCellRenderer`  
**Approach:** SSR (HTMX lazy-load nodes)

Steps:
1. Add `GET /api/cases/{id}/categories` to `iped-webapi` returning `[{id, label, count, hasChildren}]` (maps to `CategoryTreeModel`/`Category`).
2. In `WorkspaceFragmentController.sidebar()`, replace demo `CATEGORIES` list with an HTTP call to the API.
3. Each tree node renders as `<div class="node" hx-get="/workspace/sidebar/cat/{nodeId}" hx-swap="afterend" hx-trigger="click[!expanded]">` — clicking a closed caret lazy-loads children below the node.
4. Clicking a leaf/node fires `hx-get="/workspace/sidebar/cat/{nodeId}/select" hx-target="#sidebar" hx-include="[name=activeFilters]"` and also sets the results grid `query` attribute via a JS bridge (`htmx:afterSwap` event → update island attribute).
5. Active category node gets `class="node active"` from server state (tracked as a query param or server-side session).

**Test:** Fragment test with mock API returning two categories; verify expand node inserts children HTML.

---

## C-03 · Evidence Tree

**Swing origin:** `evidenceTabDock` — `JTree` with `TreeViewModel`, recursive-listing checkbox  
**Approach:** SSR (HTMX lazy-load); new sidebar tab `evid`

Steps:
1. Add `GET /api/cases/{id}/tree?path=&recursive=` to `iped-webapi` (maps to `TreeViewModel`).
2. Add `evid` to `SIDEBAR_TABS` set in `WorkspaceFragmentController`.
3. Add "Evidence" tab button in `sidebar.rocker.html`.
4. Render root nodes on first load; caret clicks lazy-load children (`hx-get="/workspace/sidebar/evid?path={encoded}"` appended after clicked node).
5. Recursive-listing toggle = `<input type="checkbox" name="recursive" hx-get="/workspace/sidebar/evid" hx-trigger="change" hx-target="#sidebar-content" hx-swap="innerHTML">`.
6. Path selection fires same JS bridge as C-02 to update the results island.

**Test:** Fragment test; verify recursive toggle changes rendered list depth.

---

## C-04 · Bookmarks Tree

**Swing origin:** `bookmarksTabDock` → `JTree` with `BookmarksTreeModel`, color-coded bookmark icons  
**Approach:** SSR shell + Angular island for edit dialog

Steps:
1. Add `GET /api/cases/{id}/bookmarks` returning `[{id, name, color, count}]`.
2. Replace the placeholder in `sidebar.rocker.html` "coll" tab with real tree nodes; render the colour as an inline CSS variable.
3. Bookmark selection: `hx-get="/workspace/sidebar/coll/{bookmarkId}/select"` + JS bridge to results island.
4. "New bookmark" / "Edit bookmark" → small Angular island `<iped-bookmark-editor>` rendered inside an HTMX-swapped modal region; outputs `bookmark-saved` CustomEvent → HTMX reloads the bookmarks tree fragment.

**Test:** Fragment test; bookmark editor island unit test.

---

## C-05 · Metadata Filter Panel

**Swing origin:** `metadataTabDock` → `MetadataPanel` — date ranges, size range, flags checkboxes, field-value facets  
**Approach:** SSR form, submit via HTMX

Steps:
1. Add `GET /api/cases/{id}/facets?fields=size,modified,mediaType,...` returning ranges and top values.
2. In "meta" tab: replace static HTML with Rocker template loops over facet fields.
3. Each range slider / checkbox: `hx-post="/workspace/filter" hx-trigger="change delay:300ms" hx-target="#topbar-chips" hx-swap="innerHTML"` to add a filter chip; JS bridge then updates results island attribute.
4. Date inputs use `<input type="date">` rendered from API min/max.

**Test:** Fragment test with facet mock data; verify that checking a flag adds a chip.

---

## C-06 · AI Filters Tree

**Swing origin:** `aiFiltersTabDock` → `JTree` with `AIFiltersTreeModel`  
**Approach:** SSR — identical pattern to C-02

Steps:
1. Add `GET /api/cases/{id}/ai-filters` endpoint.
2. Add `ai` to `SIDEBAR_TABS`; add "AI Filters" tab in `sidebar.rocker.html`.
3. Tree rendering and selection follow C-02 pattern exactly.

**Test:** Fragment test.

---

## C-07 · Active Filters Panel

**Swing origin:** `filtersTabDock` → `FiltersPanel` — lists all currently applied filters with remove buttons  
**Approach:** SSR fragment, driven by server-side filter state

Steps:
1. Add `GET /workspace/filters/active` returning an HTML fragment with all active filters as removable rows.
2. Add `filt` tab to sidebar; loads via `hx-get="/workspace/filters/active" hx-trigger="load"`.
3. Each row: `hx-delete="/workspace/filter/{id}" hx-target="#sidebar" hx-swap="innerHTML"` (reloads whole sidebar after removal) + JS bridge updates results island.
4. "Clear all" button: `hx-delete="/workspace/filters" hx-target="body" hx-swap="none"` + JS.

**Test:** Fragment test with multiple active filters; verify remove clears the row.

---

## C-08 · Results Table Island

**Swing origin:** `tableTabDock` → `ResultTableModel`, sort, column selection, check-all  
**Approach:** Existing `<iped-results-grid>` island — extend it

Steps:
1. **(Depends on API v2 prerequisite)** Delete `SearchStubController`; point island at real `POST /cases/{id}/search`.
2. Add `sort-field` and `sort-dir` inputs to the island; top-bar sort dropdown (SSR) sets them via attribute bridge.
3. Add column configuration: new SSR modal `GET /workspace/columns` renders a checklist of available columns backed by `GET /api/cases/{id}/columns`; saving posts to `/workspace/columns` (session preference) and updates island `visible-columns` attribute.
4. Check-all button in the SSR panel header dispatches `CustomEvent('check-all')` to island; island listens with `@HostListener('check-all')`.
5. Export button: island emits `export-requested` CustomEvent → HTMX opens export dialog (C-18).

**Test:** Island unit test with mock HTTP; integration test confirming sort query param is forwarded to API.

---

## C-09 · Gallery Island

**Swing origin:** `galleryTabDock` → `GalleryTable`, thumbnail loading, blur/gray filters, column resize, similar-image button  
**Approach:** New Angular island `<iped-gallery>`

Steps:
1. Create `iped-webui/src/islands/gallery/gallery.component.ts`.
2. Inputs: `case-id`, `search-id`, `columns` (default 5), `blur-filter` (boolean attr), `gray-filter` (boolean attr).
3. Outputs: `item-selected`, `selection-changed`, `similar-image-search` (with `itemId` detail).
4. Thumbnail URL: `/api/cases/{id}/items/{itemId}/thumb` — lazy-load with `IntersectionObserver`.
5. Column resize: `+`/`-` buttons in SSR panel header dispatch `CustomEvent('columns-inc')` / `CustomEvent('columns-dec')`.
6. Blur / gray toggles: SSR toolbar sends `CustomEvent('blur-toggle')` / `CustomEvent('gray-toggle')`.
7. Register as custom element in `main.ts`; add entry to island manifest.

**Test:** Island unit test; snapshot test for thumbnail grid layout.

---

## C-10 · Timeline Island

**Swing origin:** `TimelineListener` with `tableTabDock` in timeline mode  
**Approach:** New Angular island `<iped-timeline>`

Steps:
1. Create `iped-webui/src/islands/timeline/timeline.component.ts` wrapping a lightweight SVG timeline (Apache ECharts or unovis).
2. Inputs: `case-id`, `search-id`.
3. Outputs: `time-range-selected` (with `{start, end}` detail) → SSR bridge fires HTMX filter update.
4. Fetch timeline histogram from `GET /api/cases/{id}/timeline?searchId=`.
5. SSR panel header has a "Timeline" mode button in the main-panel mode segmented control; selecting it swaps the island in.

**Test:** Island unit test with mock histogram data.

---

## C-11 · Graph/Links Island

**Swing origin:** `graphDock` → `AppGraphAnalytics` (vis-based graph)  
**Approach:** New Angular island `<iped-graph>`

Steps:
1. Create `iped-webui/src/islands/graph/graph.component.ts` wrapping vis-network.
2. Inputs: `case-id`, `item-id` (seed node).
3. Outputs: `item-selected`.
4. Fetch graph data from `GET /api/cases/{id}/graph?itemId=&depth=2`.
5. SSR "Links" button in mode seg-control loads this island.

**Test:** Island unit test with mock graph data.

---

## C-12 · Info Panel — Hits Tab

**Swing origin:** `hitsDock` → `HitsTable` showing text-search hit snippets  
**Approach:** SSR fragment (HTMX)

Steps:
1. Add `GET /api/cases/{id}/items/{itemId}/hits?query=` to `iped-webapi` returning `[{text, offset}]` snippets with highlight markers.
2. In `infoPanel.rocker.html` "hits" tab: render snippet rows with `<mark>` tags.
3. Snippet row click: `hx-get="/workspace/viewer?mode=text&itemId={id}&hitOffset={offset}"` scrolls viewer to that hit.
4. Prev/next hit navigation buttons in the SSR panel header call the same endpoint with `offset` param.

**Test:** Fragment test verifying `<mark>` injection from API response.

---

## C-13 · Info Panel — Subitems / Parent / Duplicates / References / Referenced-By Tabs

**Swing origin:** `subitemDock`, `parentDock`, `duplicateDock`, `referencesDock`, `referencedByDock`  
**Approach:** SSR fragments — all share one template parameterised by tab type

Steps:
1. One shared `itemList.rocker.html` template: renders a table of `{itemId, name, size, mediaType}` rows.
2. One `GET /workspace/info/items?tab={sub|par|dup|ref|refby}&itemId=` controller method — dispatches to appropriate API endpoint per tab.
3. Each API endpoint: `GET /api/cases/{id}/items/{itemId}/{subitems|parent|duplicates|references|referencedBy}`.
4. Row click: dispatches `item-selected` JS CustomEvent on the grid island (same bridge as the main results grid).
5. HTMX pagination for large lists.

**Test:** Fragment test per tab type; verify clicking a row fires the event.

---

## C-14 · Viewer Panel — Metadata Tab

**Swing origin:** One of the viewer docks rendered by `ViewerController` — the metadata properties table  
**Approach:** SSR fragment

Steps:
1. Add `GET /api/cases/{id}/items/{itemId}/metadata` returning `[{field, value}]`.
2. In `viewer.rocker.html` "meta" mode: render as a two-column `<table>`.
3. Values that are paths/URIs: rendered as `<a hx-get="..." hx-target="#viewer-panel">` to navigate.

**Test:** Fragment test with mock metadata map.

---

## C-15 · Viewer Panel — Text Tab

**Swing origin:** `TextViewer` with hit highlighting and pagination  
**Approach:** SSR fragment; server injects `<mark>` tags

Steps:
1. Add `GET /api/cases/{id}/items/{itemId}/text?page=&query=` — server applies highlight markers server-side (maps to `TextHighlighter`).
2. In `viewer.rocker.html` "text" mode: render pre-wrapped text with `<mark class="hit">` spans; include prev/next page HTMX links.
3. `hx-push-url` keeps URL in sync so sharing a link opens the same view.

**Test:** Fragment test; verify `<mark>` tags inserted around hit terms.

---

## C-16 · Viewer Panel — Preview Tab

**Swing origin:** `MultiViewer` — renders file content (PDF, images, HTML, video) in a JavaFX WebView  
**Approach:** Angular island `<iped-preview-viewer>`

Steps:
1. Create `iped-webui/src/islands/preview/preview.component.ts`.
2. Input: `item-id`, `case-id`, `media-type`.
3. For web-native types (image/\*, video/\*, audio/\*, text/html): render in a sandboxed `<iframe src="/api/cases/{id}/items/{itemId}/content">`.
4. For PDFs: embed `<iframe src="/api/cases/{id}/items/{itemId}/content" type="application/pdf">` (browser-native) or PDF.js.
5. For unsupported types: SSR renders a fallback "Download raw" link.
6. Blur/gray filter: CSS filter on the iframe wrapper, toggled by incoming CustomEvents from SSR toolbar.
7. "Search in viewer" button: island posts `findText` message into iframe contentWindow (standard browser API).

**Test:** Island unit test; smoke test with a real PDF served from the API.

---

## C-17 · Viewer Panel — Hex Tab

**Swing origin:** Hex viewer (one of the `AbstractViewer` implementations)  
**Approach:** Angular island `<iped-hex-viewer>`

Steps:
1. Create `iped-webui/src/islands/hex-viewer/hex-viewer.component.ts`.
2. Input: `item-id`, `case-id`.
3. Virtual scroll table: fetches `GET /api/cases/{id}/items/{itemId}/content?offset=&limit=4096` as bytes.
4. Render hex + ASCII columns; highlight search matches with offset-based markers.
5. Offset navigation input; CTRL+G go-to-offset.

**Test:** Island unit test with mock byte array.

---

## C-18 · Export / Reports Dialog

**Swing origin:** `ExportFilesToZip`, `ReportDialog`, `CopyFiles`  
**Approach:** SSR modal

Steps:
1. Export button in main panel header fires `hx-get="/workspace/export/dialog" hx-target="#modal" hx-swap="innerHTML"`.
2. `GET /workspace/export/dialog`: Rocker template with scope selector (checked items / current page / all), format selector (zip/csv/report), and submit button.
3. `POST /workspace/export` starts the export job, returns a job-id; page polls `GET /workspace/export/{jobId}/status` (SSE or HTMX polling) for progress bar.
4. Completion: a download link appears in the dialog.

**Test:** Controller test verifying dialog HTML; mock job status endpoint.

---

## C-19 · Similar Search (Images / Faces / Documents)

**Swing origin:** `SimilarImagesFilterActions`, `SimilarFacesFilterActions`, similar-document search  
**Approach:** Extend C-09 (gallery island) and C-08 (results island); server-side query transform

Steps:
1. Gallery island `similar-image-search` CustomEvent → JS bridge sets results island `query` to a synthetic Lucene query with the `_imageHash` field.
2. Similar faces: same pattern using `_faceFeatures` field.
3. Similar documents: context menu on results grid (SSR fragment rendered by `hx-get="/workspace/contextmenu?itemId="`) has "Find similar documents" option → posts `similarDoc` filter to server; server constructs the MLT query.
4. Similarity threshold slider: HTMX slider in the filter chips area.

**Test:** Integration test with mock image-hash endpoint; similarity query string verified.

---

## C-20 · App Shell Layout and Resizability

**Swing origin:** Docking framework (`CControl` + `DefaultSingleCDockable`) providing drag-resizable panels  
**Approach:** CSS Grid shell with JS drag handles

Steps:
1. Main `page.rocker.html` layout is already a 4-region CSS Grid (`sidebar | main | right-stack`). Make column/row sizes CSS custom properties, saved to `localStorage`.
2. Add thin drag-handle `<div class="resize-handle">` elements between regions; `mousedown` / `mousemove` update the CSS custom properties.
3. Sidebar collapse: `<button hx-post="/workspace/layout/sidebar-collapsed" hx-swap="none">` stores preference; CSS class toggle collapses sidebar to icon-only width.
4. Vertical/horizontal layout toggle (Swing's `toggleHorizontalVerticalLayout`): swap CSS Grid template via `body.data-layout` attribute; persist in cookie.

**Test:** Browser-level resize interaction test using the preview tool.

---

## Integration sequence (suggested merge order)

```
API prerequisite
  → C-01 (top bar shell)
  → C-02, C-03, C-04, C-05, C-06, C-07  (sidebar tabs, in parallel)
  → C-08 (results grid wired to real search)
  → C-12, C-13, C-14, C-15              (info + viewer SSR tabs)
  → C-16 (preview viewer island)
  → C-09 (gallery island)
  → C-17 (hex viewer island)
  → C-10, C-11 (timeline + graph islands)
  → C-18 (export dialog)
  → C-19 (similar search)
  → C-20 (layout shell hardening)
```

Each step ships a controller + template (and optionally an island) behind a feature flag (`WebUiProperties`) so it can be enabled per deployment independently of the Swing app, which continues to run until C-20 is complete.

---

## Key invariants to preserve throughout

- **No IPED engine Maven deps in `iped-webui-server`** — all data comes over HTTP to Jersey `iped-webapi`.
- **Islands never share DOM with HTMX** — the SSR shell owns every `<div>` outside a custom element; islands own everything inside.
- **Events fire on the island host element, not `document`** — bridges must `querySelector('iped-results-grid').addEventListener(...)`.
- **`SearchStubController` is deleted** when the v2 search contract lands, not before.
