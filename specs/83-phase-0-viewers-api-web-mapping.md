# Phase 0 - `iped-viewers-api` to Web DTO/Event Mapping

## Scope
- Source focus:
  - `iped-viewers/iped-viewers-api/src/main/java/iped/viewers/api/AbstractViewer.java`
  - `iped-viewers/iped-viewers-api/src/main/java/iped/viewers/api/ResultSetViewer.java`
  - `iped-viewers/iped-viewers-api/src/main/java/iped/viewers/api/GUIProvider.java`
  - `iped-viewers/iped-viewers-api/src/main/java/iped/viewers/api/ITextParser.java`
  - `iped-viewers/iped-viewers-api/src/main/java/iped/viewers/search/HitsUpdater.java`
  - `iped-viewers/iped-viewers-api/src/main/java/iped/viewers/api/events/RowSorterTableDataChange.java`

## A. Web Viewer Contract (target)
- `canHandle(mimeType: string): boolean`
- `open(payload: ViewerOpenRequest): ViewerOpenResponse`
- `search(payload: ViewerSearchRequest): ViewerSearchResponse`
- `navigateHit(payload: ViewerNavigateHitRequest): ViewerHitState`
- `setToolbarState(payload: ViewerToolbarStateRequest): ViewerToolbarState`
- `copySnapshot` is replaced by:
  - `GET /cases/{caseId}/items/{itemId}/renditions/image` or client-side screenshot capability.

## B. Method Mapping
- `AbstractViewer.isSupportedType(contentType)`:
  - Web: `canHandle(mimeType)`.
- `AbstractViewer.loadFile(content, contentType, highlightTerms)`:
  - Web: `open(ViewerOpenRequest)` + backend rendition endpoints.
- `AbstractViewer.searchInViewer(term, hitsUpdater)`:
  - Web: `POST /viewer/sessions/{id}/search`.
- `AbstractViewer.scrollToNextHit(forward, ...)`:
  - Web: `POST /viewer/sessions/{id}/hits:navigate`.
- `AbstractViewer.getHitsSupported()`:
  - Web capability field: `hitsMode` enum (`none|external|internal`).
- `AbstractViewer.getToolbarSupported()/setToolbarVisible()`:
  - Web capability/state: `toolbar.supported`, `toolbar.visible`.
- `ResultSetViewer.updateSelection()/checkAll()/notifyCaseDataChanged()`:
  - Web events + commands on result context and selection APIs.

## C. Proposed DTOs

```json
{
  "ViewerOpenRequest": {
    "caseId": "string",
    "itemId": "string",
    "mimeType": "string",
    "highlightTerms": ["string"],
    "preferredViewerId": "string|null",
    "context": {
      "queryId": "string|null",
      "selectedRow": 0
    }
  }
}
```

```json
{
  "ViewerOpenResponse": {
    "viewerSessionId": "string",
    "viewerId": "string",
    "capabilities": {
      "search": true,
      "hitsMode": "external",
      "toolbar": { "supported": true, "visible": false }
    },
    "renditions": [
      { "kind": "text", "url": "/cases/{caseId}/items/{itemId}/renditions/text" },
      { "kind": "html", "url": "/cases/{caseId}/items/{itemId}/renditions/html" }
    ]
  }
}
```

```json
{
  "ViewerSearchRequest": {
    "term": "string",
    "matchMode": "contains",
    "caseSensitive": false
  },
  "ViewerSearchResponse": {
    "totalHits": 0,
    "currentHit": 0,
    "hitRanges": [
      { "start": 0, "end": 0, "page": 0 }
    ]
  }
}
```

```json
{
  "ViewerNavigateHitRequest": {
    "direction": "next",
    "wrap": false
  },
  "ViewerHitState": {
    "totalHits": 0,
    "currentHit": 0
  }
}
```

## D. Proposed Events (SSE/WebSocket)
- `viewer.session.opened`
- `viewer.session.closed`
- `viewer.capabilities.changed`
- `viewer.hit.updated` (maps `HitsUpdater.updateHits(currHit, totHits)`)
- `viewer.search.completed`
- `results.selection.changed` (maps `ResultSetViewer.updateSelection`)
- `results.checkall.changed` (maps `ResultSetViewer.checkAll`)
- `results.sort.changed` (maps `RowSorterTableDataChange.sortKeys`)
- `case.data.changed` (maps `ResultSetViewer.notifyCaseDataChanged`)
- `layout.changed` (from DockingFrames replacement state)

## E. `GUIProvider` Replacement in Web
- `createFileDialog`:
  - Replace with browser download/upload intents plus backend job endpoints.
- `getColumnsManager`:
  - Replace with `GET/PUT /users/{userId}/preferences/columns`.
- `getSelectedBookmarks` and `getSelectedCategories`:
  - Replace with state query endpoints:
    - `GET /cases/{caseId}/selection/bookmarks`
    - `GET /cases/{caseId}/selection/categories`

## F. Compatibility and Gaps
- Swing object references (`JPanel`, `JTable`, Dockable handles, clipboard ownership) are non-portable and removed from web contracts.
- `ITextParser` large-file/hit structures should be exposed as backend service metadata:
  - parser progress, parsed ranges/pages, hit offsets.
- `copyScreen()` should not be emulated server-side by default; use deterministic renditions for evidentiary reproducibility.

## G. Phase 1 Contract Backlog (ready to implement)
- Define OpenAPI schemas for:
  - `ViewerOpenRequest/Response`
  - `ViewerSearchRequest/Response`
  - `ViewerNavigateHitRequest/ViewerHitState`
  - Result selection/check/sort event payloads.
- Define event schema versioning:
  - `eventType`, `version`, `timestamp`, `caseId`, `sessionId`, `payload`.
