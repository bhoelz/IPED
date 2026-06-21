# iped-webui-server

Server-side composition root for the IPED web UI.

Spring Boot owns routing, page layout, server-rendered templates (Rocker) and
HTMX fragment endpoints, and re-exposes `/api/**` by proxying to the Jersey
`iped-webapi`. Angular survives only as embedded **custom-element "islands"**
mounted inside SSR pages — never as the app shell.

This is the **vertical-slice pilot**: one SSR page (`/workspace`) with HTMX
regions and one Angular island (the results grid), proving the architecture and
the boundary rules end-to-end.

## Architecture

```
Browser  ─ full page GET /workspace ─▶ Spring MVC ─▶ Rocker page (layout + HTMX regions + island host)
         ─ HTMX fragment GET/POST   ─▶ Spring MVC ─▶ Rocker partials (HTML only)
         ─ island JSON fetch /api/** ─▶ Spring proxy ─▶ Jersey iped-webapi ─▶ iped-engine
```

Two rendering runtimes in **adjacent boxes**: Rocker fragments on one side, the
Angular custom element on the other. Neither patches the other's DOM subtree.

## Boundary rules (must hold)

- **Spring** owns page URLs, auth, layout, CSRF, navigation state.
- **HTMX** owns server-rendered fragments — forms, side panels, validation,
  tabs, detail panes. Response is HTML.
- **Angular** owns only the island root, its internal DOM, local UI state, and
  JSON calls. The SSR shell must not render or patch inside the island subtree.
- **No shared mutable client state** across the boundary. Communication goes
  through DOM CustomEvents, URL/query params, or the backend only.
- **No duplicate routing** — islands have no router.
- **No duplicate fetching** — if the island owns grid data over JSON, HTMX must
  not also fetch that grid as HTML.
- Design-system tokens are shared via **CSS variables only**; component
  rendering ownership stays separate.

### Decision rule: HTMX or Angular?

| The interaction…                              | Use     | Returns |
|-----------------------------------------------|---------|---------|
| updates server-owned markup                   | HTMX    | HTML    |
| lives entirely inside the rich widget         | Angular | JSON    |
| would make both co-own one DOM subtree        | neither — redesign |

## Island contract

Islands are framework-agnostic custom elements (Angular Elements):

- **Inputs are kebab-cased attributes**: `<iped-results-grid case-id="…"
  search-id="…" api-base="/api">`.
- **Outputs are DOM CustomEvents dispatched on the host element** (they do **not**
  bubble): `item-selected`, `selection-changed`. SSR/HTMX bridges must
  `addEventListener` on the island element, not on `document`.

Island source lives in `iped-webui/src/islands/`. Register each new island with
one `customElements.define` in `src/islands/main.ts`.

## Layout & routing

| Route                              | Owner                        | Returns           |
|------------------------------------|------------------------------|-------------------|
| `GET /` → `/workspace`             | `WorkspaceController`        | redirect          |
| `GET /workspace`                   | `WorkspaceController`        | full Rocker page  |
| `GET /workspace/sidebar`, `/info`  | `WorkspaceFragmentController`| HTML partials     |
| `/api/**`                          | `ApiProxyController`         | proxied JSON      |
| `/islands/**`, `/vendor/**`        | static handler               | hashed JS/CSS, htmx |

## Asset loading

The Angular `islands` build (`npm run build:islands`) emits **content-hashed**
bundles to `iped-webui/dist/islands/browser/`. The Maven build copies them into
`classpath:/static/islands/`, and `IslandManifest` resolves the hashed filenames
at startup so the Rocker layout injects the right `<script type="module">`
without hard-coding hashes. Bundles are served immutable with a 1-year max-age.

## Build & run

```bash
# Full build incl. Angular island bundle (downloads Node via frontend plugin)
mvn -pl iped-webui-server package

# Server only, reuse a pre-built island (skip the Node/Angular step)
npm --prefix ../iped-webui run build:islands     # once, to produce the bundle
mvn -pl iped-webui-server -DskipTests -Dwebui.skipFrontend=true package

java -jar target/iped-webui-server-*.jar          # serves http://localhost:8090/workspace
```

Point the proxy at a running `iped-webapi` with
`IPED_WEBAPI_BASE_URL` (default `http://localhost:8080`).

## Temporary pilot stub

`SearchStubController` serves contract-shaped `SearchResultsPage` JSON for
`POST /api/cases/{caseId}/search` and `…/results`, because the live Jersey
`iped-webapi` still only implements the legacy `GET /search?q=` endpoint. It
takes precedence over the `/api/**` proxy. **Delete it** once the v2 search
contract is implemented against the engine (EPIC-WEB-03 in
`specs/87-web-ui-delivery-backlog.md`).

## Migration path

Add islands incrementally (viewer, link graph) the same way — each a custom
element with an attributes-in / events-out contract; the SSR shell grows HTMX
regions around them. Defer any real micro-frontend platform (module federation,
independent deploys) until multiple teams or release cadences actually require it.

## Roadmap

See [iped-webui-server-ROADMAP.md](../docs/roadmaps/iped-webui-server-ROADMAP.md) for planned work, current phase status, and linked issues.

## Design docs

- [FRONTEND_IMPLEMENTATION_PLAN.md](FRONTEND_IMPLEMENTATION_PLAN.md) — per-component Swing-origin → web-approach mapping for the main window migration (historical design rationale; most components are already implemented).
- [REFACTORING_PLAN.md](REFACTORING_PLAN.md) — history of the SSR + HTMX + Angular-islands pilot (architecture decisions, Spring Boot 4 migration notes); the boundary rules it documents are now also stated above as the current standing rules.
- [SECURITY-FIX-PLAN.md](SECURITY-FIX-PLAN.md) — security review findings and remediation status. Most are resolved; the one still-open finding is tracked as ISSUE-446 in the roadmap above.
