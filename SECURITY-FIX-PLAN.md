# Security Fix Plan — `feature/kafka-distributed-processing`

Status: **Draft / Open**
Branch: `feature/kafka-distributed-processing`
Date: 2026-06-09
Owner: _unassigned_

This plan tracks remediation of the security findings from the branch security
review. Three XSS vulnerabilities were confirmed at high confidence in the new
web UI; two additional findings are tracked as "harden before shipping" items
because they are not currently reachable in the shipped deployment.

The web UI (`iped-webui-server`) ships **without** Spring Security, CSRF
protection, or authentication. This amplifies every XSS finding below, because a
successful script injection runs in the analyst's session with full access to
the unauthenticated `/api/**` surface. Adding an authentication/CSRF baseline
(Task 0) is therefore a prerequisite, not an optional hardening step.

---

## Summary of findings

| # | Severity | Category | Location | Status |
|---|----------|----------|----------|--------|
| 1 | High | XSS — DOM event-handler context | `iped-webui-server` Rocker `on*` handlers | Open |
| 2 | High | XSS — stored (sanitizer bypass) | `iped-webui` Angular viewer | Open |
| 3 | Medium | XSS — reflected | `WorkspaceFragmentController.startExport` | Open |
| 0 | High (prereq) | Missing authn / CSRF baseline | `iped-webui-server` | Open |
| 4 | Low / deferred | Plugin signature bypass (no prod caller) | `iped-engine` registry skeleton | Deferred |
| 5 | Low / deferred | Config API arbitrary-path write (not started in prod) | `iped-engine` config API | Deferred |

---

## Task 0 — Add authentication + CSRF baseline to `iped-webui-server` (prerequisite)

**Why:** All three XSS issues escalate to full session/API compromise precisely
because the web UI server has no auth or CSRF. The README claims "Spring owns
auth/CSRF" but no `spring-boot-starter-security` dependency or
`SecurityFilterChain` exists.

**Steps:**
1. Add `spring-boot-starter-security` to `iped-webui-server/pom.xml`.
2. Add a `SecurityFilterChain` config:
   - Require authentication for all routes (mechanism per deployment — form
     login, reverse-proxy header auth, or SSO).
   - Enable CSRF protection. Configure HTMX to send the CSRF token
     (e.g. via a meta tag + `hx-headers`, or the Spring `CookieCsrfTokenRepository`).
3. Set secure response headers (CSP, `X-Content-Type-Options: nosniff`,
   `X-Frame-Options`/frame-ancestors). A restrictive **Content-Security-Policy**
   (no inline scripts, restricted `script-src`) is strong defense-in-depth
   against the XSS findings below.

**Acceptance:** Unauthenticated requests to `/workspace/**` are rejected;
state-changing POSTs without a valid CSRF token are rejected; a baseline CSP
header is present on rendered pages.

---

## Task 1 — Fix DOM event-handler XSS in Rocker templates (Finding #1, High)

**Files:**
- `iped-webui-server/src/main/rocker/views/workspace/fragments/itemList.rocker.html:55`
- `iped-webui-server/src/main/rocker/views/workspace/fragments/sidebar.rocker.html:52,70,93,174,183`
- `iped-webui-server/src/main/rocker/views/workspace/fragments/evidenceChildren.rocker.html:8`

**Root cause:** Untrusted values (`@r.iid()`, `@r.name()`, `@caseId`,
`@cat.name()`, `@bm.name()`) are interpolated inside JavaScript string literals
within inline `onclick="..."` handlers. Rocker applies only HTML-entity
escaping, which the browser decodes back to raw characters *before* the JS
parser runs — so a `'` breaks out of the string literal.

**Preferred fix (data-attribute + delegation):**
1. Replace inline `onclick="ipedSelectItem('@r.iid()')"` with
   `data-*` attributes, e.g.:
   ```html
   <tr class="item-row" data-iid="@r.iid()" ...>
   ```
   (HTML-attribute context — Rocker's entity escaping *is* sufficient here.)
2. Attach behavior once via event delegation in the page script:
   ```js
   document.addEventListener('click', (e) => {
     const row = e.target.closest('.item-row[data-iid]');
     if (row) ipedSelectItem(row.dataset.iid);
   });
   ```
3. Apply the same pattern to every `on*` handler that interpolates a value:
   `ipedSelectFilter`, `ipedEvidToggle`, `ipedAiToggle`.

**Alternative (if inline handlers must stay):** JS-string-encode each value
(e.g. JSON-encode, or escape `'` → `\x27`, `"` → `\x22`, `\` → `\\`, `<`/`>`,
newlines) via a dedicated Rocker helper before emitting into the handler. Do
**not** rely on default HTML escaping for JS contexts.

**Note:** `sidebar.rocker.html:34` uses only static literals — no change needed.

**Acceptance:** Injecting `'-alert(1)-'` (or `");alert(1)//`) as an item name,
category name, bookmark name, or `caseId` param no longer executes; the value
appears inert in the rendered handler/data attribute.

---

## Task 2 — Fix stored XSS in Angular viewer (Finding #2, High)

**Files:**
- `iped-webui/src/app/domains/viewer/data-access/viewer.facade.ts:196-204`
- `iped-webui/src/app/domains/workspace/pages/workspace-page.html:762`

**Root cause:** The HTML rendition of an item (streamed **as-is** from raw
evidence bytes by `HtmlWebRenderer`) is passed through
`bypassSecurityTrustHtml(html)` and bound via `[innerHTML]` into a plain `<div>`
in the app origin — disabling Angular's sanitizer entirely.

**Preferred fix (sandboxed iframe — mirror the SSR UI):**
1. Stop using `bypassSecurityTrustHtml` + `[innerHTML]` for the HTML branch.
2. Render the rendition inside a sandboxed iframe, matching the existing SSR
   template `viewer.rocker.html:70-74`. Prefer the rendition URL as `src`:
   ```html
   <iframe [src]="viewerFacade.htmlRenditionUrl()"
           sandbox="allow-popups"
           referrerpolicy="no-referrer"
           style="flex:1;border:0"></iframe>
   ```
   Tighten the `sandbox` allowlist as far as the preview allows. Avoid combining
   `allow-same-origin` with `allow-scripts` (that pairing lets framed script
   reach the parent origin). The SSR template currently uses
   `allow-same-origin allow-scripts` — review and tighten that separately.
3. Remove the now-unused `bypassSecurityTrustHtml` call from `viewer.facade.ts`.

**Alternative (server-side sanitization):** Sanitize the rendition on the
server before delivery (allowlist-based HTML sanitizer / DOMPurify-equivalent),
so the as-is stream from `HtmlWebRenderer` is never returned raw. This also
protects any other consumer of the endpoint.

**Acceptance:** Opening an item whose HTML rendition contains
`<img src=x onerror=...>` / `<svg onload=...>` does not execute script in the
web-UI origin.

---

## Task 3 — Fix reflected XSS in export modal (Finding #3, Medium)

**File:** `iped-webui-server/src/main/java/iped/webui/web/WorkspaceFragmentController.java:287-317`
(sink at line 316 for `scope`, line 305 for `format`)

**Root cause:** `startExport` builds a raw HTML response with
`"""...%s...""".formatted(format.toUpperCase(), scope)` returned as
`@ResponseBody` with `produces=text/html`, bypassing Rocker auto-escaping.
`scope` is reflected verbatim; `format` is only upper-cased.

**Preferred fix (use a Rocker template):**
1. Move the modal HTML into a Rocker fragment (e.g. `exportStatus.rocker.html`)
   so `scope`/`format` are auto-escaped, consistent with every other handler in
   the class (which call `*.template(...).render()`).
2. Validate `format` against an allowlist (`zip`, `csv`, ... ) and reject
   anything else.
3. Validate/normalize `scope` against the known set (`checked`, `all`, ...).

**Alternative (if kept inline):** HTML-encode `scope` and `format` before
interpolation with a proper encoder; do not return hand-built HTML strings.

**Acceptance:** Posting `scope=<img src=x onerror=alert(1)>` returns the value
HTML-escaped (inert); unknown `format`/`scope` values are rejected.

---

## Task 4 — Harden plugin registry signature verification before wiring in (Finding #1 from review, deferred)

**Files:** `iped-engine-parent/iped-engine-core/src/main/java/iped/engine/config/registry/`
(`SignatureVerifier.java:12-15`, `RegistryClient.java:52`,
`RegistrySchemaValidator.java:18`, `PluginVerifier.java:50-63`,
`JsonCanonicalizer.java:9`)

**Status:** **Deferred — not a live vulnerability.** The registry package has no
production caller (referenced only by its own tests and the roadmap; install
flow integration is "Não Iniciada"). However, the verification logic is broken
and **must be fixed before the install flow is wired up**, or it becomes a
supply-chain RCE.

**Steps (do before enabling the install flow):**
1. Remove the `type == "none"` unconditional pass in
   `SignatureVerifier.verifyDetached`; treat unsigned as invalid in production.
2. Restrict `signature.type` to a cryptographic allowlist (e.g. `SHA256withRSA`)
   in `RegistrySchemaValidator` — not just "non-blank".
3. Make the per-artifact signature **mandatory**
   (`RegistryClient.validateDownloadedArtifact` currently verifies it only
   `if (version.signature() != null)`).
4. Implement real JSON canonicalization (`JsonCanonicalizer.canonicalize` is a
   `json.trim()` placeholder), anchoring trust in the pinned `PublicKeyStore`.

**Acceptance:** A registry index with `type:"none"` or a missing artifact
signature is rejected; only artifacts signed by a pinned trusted key load.

---

## Task 5 — Harden Configuration API file write before exposing it (Finding #5 from review, deferred)

**Files:**
- `iped-engine-parent/iped-engine-core/src/main/java/iped/engine/config/api/ConfigurationResource.java:90-106`
- `iped-engine-parent/iped-engine-core/src/main/java/iped/engine/config/api/ConfigurationAPIController.java:223-245`

**Status:** **Deferred — not started in shipped deployment.** `ConfigurationServer`
is a dev/`api-only`-profile tool; the production coordinator runs
`CoordinatorServer` and the worker runs `TaskAgentLauncher`. Fix before the
config API is ever exposed.

**Steps:**
1. Confine `backupPath` (and the `saveConfiguration` path, which is a worse
   primitive — fully attacker-controlled path *and* filename) to a configured
   base directory; reject absolute paths and `..` segments after
   canonicalization.
2. Add authentication/authorization to `ConfigurationServer` (Jersey/Jetty
   currently binds all interfaces with no auth filter) before any deployment
   exposes it.

**Acceptance:** Path traversal / absolute paths are rejected; the endpoint is
not reachable unauthenticated.

---

## Suggested sequencing

1. **Task 0** (auth + CSRF + CSP baseline) — reduces blast radius of everything else.
2. **Tasks 1, 2, 3** (the three confirmed XSS fixes) — can proceed in parallel.
3. **Tasks 4, 5** — schedule alongside the features that would make them reachable
   (plugin install flow; config API exposure); gate those features on completion.

## Verification

- Add regression tests for each XSS sink (template rendering / controller
  response assertions that injected payloads are escaped or rejected).
- For the Angular viewer, add a test that a rendition containing an
  event-handler vector does not execute (iframe isolation in place).
- Re-run the branch security review after fixes to confirm the sinks are closed.
