# iped-viewers — Evolution Roadmap

> Module purpose: content viewers — `iped-viewers-api` (viewer contracts),
> `iped-viewers-impl` (Swing/native viewers incl. LibreOffice embedding),
> `iped-viewers-web` (web-renderable viewer output).
> Status legend: `[ ]` planned · `[~]` in progress · `[x]` done

## Current state (2026-06)
- Swing-first viewer set in `iped-viewers-impl`; web mapping inventoried in
  `specs/83-phase-0-viewers-api-web-mapping.md` (Phase 0 of the rewrite spec).
- `iped-viewers-web` exists as the target for browser-rendered viewing.

## Phase 1 — Contract-first viewer model
- [x] Finalize the viewer capability contract in `iped-viewers-api`: MIME routing,
      fallback chain, rendering target (web HTML / native bridge), lifecycle events
      (open/loading/ready/error/closed per root roadmap workstream 2).
      (New `iped.viewers.api.capability` package:
      `RenderTarget` enum — `WEB_HTML`, `NATIVE_SWING`, `NATIVE_BRIDGE`.
      `ViewerLifecycleEvent` enum — `OPEN`, `LOADING`, `READY`, `ERROR`, `CLOSED`.
      `ViewerClassification` enum — `WEB_PORTABLE`, `WEB_PORTABLE_WITH_WORK`, `NATIVE_ONLY`.
      `ViewerCapabilityDescriptor` — immutable builder record: name, MIME set, render target,
      classification, hits/search/toolbar flags, classification note.
      `IViewerCapability` — interface for viewers to self-declare their descriptor;
      optional, does not break existing `AbstractViewer` impls.
      `IViewerLifecycleListener` — `@FunctionalInterface` callback for OPEN/LOADING/READY/
      ERROR/CLOSED transitions; maps to Angular island `viewerReadyEvent`/`islandErrorEvent`.)
- [x] Classify every existing viewer: web-portable, web-portable-with-work, or
      native-only (input: the Phase 0 mapping spec; output: a tracked matrix here).
      (Classification matrix written to `specs/95-viewer-classification-matrix.md`.
      WEB_PORTABLE: ATextViewer, HtmlViewer, HtmlLinkViewer, ImageViewer, IcePDFViewer,
      PDFBoxViewer, HexViewerPlus, MetadataViewer — all already covered by islands or
      iped-webapi endpoints.
      WEB_PORTABLE_WITH_WORK: AudioViewer (codec/transcode), TiffViewer (server-side
      conversion), EmailViewer (EML→HTML), MsgViewer (MSG→HTML + CID rewriting).
      NATIVE_ONLY: CADViewer (proprietary SDK), LibreOfficeViewer (companion bridge target),
      ReferencedFileViewer (OS shell, companion bridge).
      MIME fallback chain and retirement criteria also documented.)

## Phase 2 — Web viewer buildout (5.0 workstream 1)
- [x] Priority web viewers: text, HTML (sanitized), image, PDF, basic email — served
      through `iped-webapi` and rendered in the browser UI / SSR islands.
      (`<iped-viewer>` island (iped-webui Phase 5) MIME-routes to: text (fetches
      `TextV2` as `text/plain`), HTML (fetches as `text/html`, renders via `[innerHTML]`
      with DOMPurify), image (direct `<img>` from `ContentV2`), PDF (`<embed type=pdf>`),
      hex (delegates to `<iped-hex-viewer>` for binary/unknown). Basic email bodies are
      rendered via the HTML/text path; multi-part EML assembly is WEB_PORTABLE_WITH_WORK
      per the classification matrix.)
- [x] Hex viewer and metadata viewer as web components (already prototyped in the SSR
      workspace fragments — promote to real implementations).
      (`<iped-hex-viewer>` is a production Angular island with byte-range pagination
      via `ContentV2` (Range: header support added in iped-webapi Phase 3). Metadata
      panel is the HTMX info-panel fragment served by `WorkspaceFragmentController.info()`
      — real endpoint, no stub data.)
- [x] Media playback (video/audio) with server-side transcode fallback for non-browser
      codecs.
      (`audio/*` routing: native `<audio controls>` from `ContentV2`; transcription/
      confidence/duration fetched from item metadata. Browser-native codecs: MP3, AAC,
      OGG, WAV, FLAC, WebM audio.
      `video/*` routing: native `<video controls>` from `ContentV2`. Browser-native
      codecs: MP4/H.264, WebM/VP8/VP9, OGG/Theora.
      Server-side fallback: `ContentV2?transcode=webm` shells out to ffmpeg (audio →
      libopus/WebM; video → VP9+Opus/WebM). Viewer `mediaUrl()` computed appends
      `?transcode=webm` for non-browser-native MIME types (AMR, WMA, 3GPP, SILK, AVI,
      WMV, MOV, …). Returns 501 when ffmpeg is not on PATH. ffmpeg feeder runs as a
      daemon thread to avoid blocking the Jersey worker.)
- [x] Email/MSG viewer in browser.
      (`message/rfc822`, `message/x-emlx`, `message/*`, `application/vnd.ms-outlook`
      routed to `'email'` viewer type. Two parallel requests via `forkJoin`: item metadata
      for headers (Subject/From/To/CC/BCC/Date from Tika metadata keys `Message-Subject`,
      `Message:Message-From/To/Cc/Bcc`, `dcterms:created`) and text endpoint (`Accept:
      text/html`) for the rendered body. Header grid + separator + `[innerHTML]` body.
      CID inline-image rewriting is a documented follow-up.)
- [x] TIFF → PNG server-side conversion for browser rendering.
      (`ContentV2` extended with `?format=png`: `ImageIO.read()` → `ImageIO.write(png)`
      using JDK 9+ built-in TIFF reader (`com.sun.imageio.plugins.tiff`). Returns 422
      for non-image content. Viewer island `imageUrl()` computed appends `?format=png`
      for `image/tiff` / `image/x-tiff`; other image types use the raw `contentUrl()`.)
- [x] Sanitization/security review for rendering hostile evidence content in a browser
      context (CSP, sandboxed iframes, no script execution from evidence).
      (CSP policy added in iped-webui-server Phase 3: `frame-src 'self'` for sandboxed HTML
      renditions, `object-src 'self'` for PDF `<embed>`, `'unsafe-inline'` styles only for
      Rocker SSR, `data:` and `blob:` for gallery thumbnails. `SecurityConfig` also sets
      `X-Frame-Options: DENY`, `X-XSS-Protection`, Referrer-Policy, and Permissions-Policy.
      `<iped-evidence>` guard in iped-mcp prevents prompt injection for AI paths.)

## Phase 3 — Companion app bridge (5.0 workstream 2)
- [ ] Native-only viewers (LibreOffice embedding first) exposed through the companion
      desktop app handoff protocol; each bridged viewer gets a retirement criterion.
- [ ] Keep Swing implementations functional until browser/companion parity is validated
      per viewer (parity tests on a golden item set).

## Phase 4 — Decommission path
- [ ] Per-viewer parity sign-off checklist; remove Swing-only viewers from the default
      distribution once their replacement is signed off.

## Progress checks
- Viewer matrix in this file updated as classifications/parities land.
- Browser UI renders the priority-five viewer set against a real case.
