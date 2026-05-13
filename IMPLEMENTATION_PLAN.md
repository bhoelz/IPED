## IPED Web Modernization Roadmap (Angular 21 + PrimeNG 21)

### Summary
Port `iped-app` Swing UI to a browser frontend in phases, keeping `iped-engine` and heavy parsing/index/search logic on backend services.  
Chosen defaults:
- Deployment target: **remote server first**
- MVP focus: **search + results table + basic viewers**

This plan uses `iped-viewers-api` as the conceptual contract baseline, but defines a web-native API and viewer plugin model to replace Swing coupling (`JPanel`, DockingFrames, AWT dialogs/events).

### Implementation Changes
1. Architecture baseline
- Create new modules:
  - `iped-web-frontend` (Angular 21 + PrimeNG 21)
  - `iped-web-backend` (Java service façade over `iped-engine`, `iped-parsers`, viewer renderers)
- Keep `iped-engine` as core domain/search authority.
- Add stateless REST APIs for query/data and streaming endpoints for long-running tasks/events.

2. UI shell migration (from `App`/DockingFrames)
- Replace DockingFrames (`CControl`, `DefaultSingleCDockable`) with browser layout:
  - PrimeNG `Splitter`, `TabView`, `Tree`, `Table`, `Panel`, `Dialog`, `Toast`
- Port these first-class areas:
  - Search bar + filters
  - Result table + paging/sort/selection
  - Left trees (categories/bookmarks/filters)
  - Evidence/viewer panel
- Save/restore layout and user prefs server-side (or per-user profile).

3. Viewer strategy (from `ViewerController` + `MultiViewer`)
- Build a web viewer registry keyed by MIME/content type (same concept as `AbstractViewer.isSupportedType`).
- Standardize viewer contract for web:
  - `canHandle(mimeType)`
  - `open(itemId, renditionOptions)`
  - `search(term)` / `nextHit()` / `prevHit()`
- Viewer migration classification:
  - Straightforward:
    - `TextViewer`, `HtmlViewer`, `ImageViewer`, `MetadataViewer`
  - Moderate:
    - `EmailViewer`, `MsgViewer`, `AudioViewer`, `ReferencedFileViewer`
  - Tricky/high-risk:
    - `HexViewerPlus` (binary navigation + highlight semantics)
    - `LibreOfficeViewer` (currently native/UNO/SWT)
    - `CADViewer` (format/rendering complexity)
    - graph analytics (`org.kharon`) and timeline (`JFreeChart` custom stack)

4. Backend API surface (minimum target)
- Case/session:
  - `POST /cases/{caseId}/sessions`
  - `GET /cases/{caseId}/metadata`
- Search/query:
  - `POST /cases/{caseId}/search` (query, filters, sort, page)
  - `GET /cases/{caseId}/search/{searchId}/results`
  - `POST /cases/{caseId}/search/{searchId}/facets`
- Selection/context:
  - `GET /cases/{caseId}/items/{itemId}`
  - `GET /cases/{caseId}/items/{itemId}/relationships`
- Viewer/renditions:
  - `GET /cases/{caseId}/items/{itemId}/renditions/text`
  - `GET /cases/{caseId}/items/{itemId}/renditions/html`
  - `GET /cases/{caseId}/items/{itemId}/renditions/image`
  - `GET /cases/{caseId}/items/{itemId}/renditions/pdf`
  - `GET /cases/{caseId}/items/{itemId}/bytes` (range requests for hex/media)
- Async/jobs/events:
  - `POST /jobs/export`, `GET /jobs/{jobId}`
  - `GET /events` (SSE/WebSocket for progress, cancellation, status)

5. Recommended web libraries for tricky components
- Data grid at Swing parity: **AG Grid Enterprise** (or PrimeNG Table + CDK virtual scroll if acceptable tradeoffs)
- Binary/hex viewer: **hex-editor component based on TypedArray + virtualized rows** (or Monaco custom binary model)
- Graph analytics replacement for `kharon`: **Cytoscape.js** (or Sigma.js for very large graphs)
- Timeline replacement for custom JFreeChart stack: **Apache ECharts** or **Highcharts Stock**
- Document rendering:
  - PDF: **PDF.js**
  - Office docs: backend conversion to HTML/PDF via LibreOffice headless
  - Email/MSG: backend normalization to safe HTML + attachment API
- Search highlights:
  - Text/HTML highlighting with backend offset maps + client overlays

6. Sequenced delivery phases
- Phase 0: Discovery + contracts
  - Inventory all Swing actions, shortcuts, and states from `App`/`ViewerController`.
  - Map `iped-viewers-api` contracts to web DTOs/events.
- Phase 1: Platform skeleton
  - Auth/session, case open, search endpoint, frontend shell, routing/state store.
- Phase 2: MVP functional slice
  - Query/filter/result table
  - Item details + metadata
  - Viewers: text/html/image/pdf/email-basic
  - Bookmark/category selection and saved filters (core subset)
- Phase 3: Analyst parity
  - Advanced filters, bulk operations/export, keyboard workflows, richer email/msg handling.
- Phase 4: Advanced analytics
  - Timeline module and graph module replacement.
- Phase 5: Long-tail viewers
  - Hex, CAD, office-native edge cases, full parity hardening.

### MVP Milestone (Fast First Delivery)
Deliver in 8-12 weeks:
- Open case, run search, paginate/sort/filter results
- Show metadata panel and core trees (categories/bookmarks)
- Open selected result in web viewers:
  - text, html, image, pdf, basic email view
- Progress/status events for long operations
- Export selected items as async job

Explicitly out of MVP:
- Graph analytics (`iped.app.graph.*`)
- Full timeline module (`iped.app.timelinegraph.*`)
- Full hex parity
- Native LibreOffice embedded viewer behavior

### Test Plan
- Contract tests:
  - API schema/DTO compatibility for search, results, renditions, jobs
- Golden dataset parity:
  - Same query/filter in Swing vs web returns same IDs/order (within defined tolerance)
- Viewer validation:
  - MIME routing and fallback behavior
  - Highlight/hit navigation correctness in text/html/pdf
- Performance:
  - 100k+ result datasets with virtual scrolling/paging targets
- Security:
  - HTML sanitization, content-disposition, access controls, audit logs

### Assumptions
- `iped-engine` remains backend-only and authoritative.
- Viewer rendering for complex formats is server-side when browser-native rendering is weak.
- Existing `iped-viewers-api` is a reference for behavior, not reused as-is due to Swing/AWT types.
- PrimeNG is primary component library; non-Prime libraries are allowed for graph/timeline/grid where parity requires it.
