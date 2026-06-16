# Viewer Classification Matrix

> Input: `specs/83-phase-0-viewers-api-web-mapping.md` (Phase 0 inventory)
> Output: per-viewer classification for the web-viewer buildout (iped-viewers Phase 2)
> Legend: WP = WEB_PORTABLE · WPW = WEB_PORTABLE_WITH_WORK · NO = NATIVE_ONLY

## Classification rules

| Class | Criterion |
|-------|-----------|
| **WEB_PORTABLE** | Content type is directly renderable in a modern browser; a web island already exists or only a thin API endpoint is needed; no format conversion, codec negotiation, or native SDK is required. |
| **WEB_PORTABLE_WITH_WORK** | Content type is browser-renderable in principle, but the current implementation relies on a Java/Swing subsystem that requires porting, a conversion pipeline, a security/sanitization review, or a missing island component. |
| **NATIVE_ONLY** | Rendering requires native execution (JNI, COM, proprietary SDK, file system access, or a privilege boundary that cannot be crossed from the browser). Must be served via the Swing desktop app or the companion-app bridge. |

---

## Viewer matrix

| Viewer class | Primary MIME range | Classification | Render target | Notes / work items |
|---|---|---|---|---|
| `ATextViewer` | `text/plain`, `text/*` | WP | WEB_HTML | Text island (`<iped-viewer type=text>`) already in Phase 5 of iped-webui. No extra work. |
| `HtmlViewer` | `text/html` | WP | WEB_HTML | HTML island renders via `[innerHTML]` with DOMPurify sanitization already in place. |
| `HtmlLinkViewer` | `text/html` + hyperlink intercept | WP | WEB_HTML | Hyperlink-intercept layer needed in the island (open in sandboxed tab, not current frame). |
| `ImageViewer` | `image/*` | WP | WEB_HTML | Image mode in `<iped-viewer>` serves a direct `<img>` from `ContentV2`. Done. |
| `IcePDFViewer` | `application/pdf` | WP | WEB_HTML | `<embed type="application/pdf">` or PDF.js; `<iped-viewer type=pdf>` already wires this. |
| `PDFBoxViewer` | `application/pdf` | WP | WEB_HTML | Same target as IcePDFViewer. PDFBox used server-side for text extraction only; browser renders PDF natively. |
| `HexViewerPlus` | `*/*` (binary fallback) | WP | WEB_HTML | Hex island (`<iped-hex-viewer>`) already in iped-webui Phase 2. Byte-range via `ContentV2` (Phase 3 of iped-webapi). |
| `MetadataViewer` | N/A (metadata panel) | WP | WEB_HTML | Metadata table rendered by HTMX fragment (`WorkspaceFragmentController.viewer` meta mode). Done. |
| `AudioViewer` | `audio/*` | WPW | WEB_HTML | `<iped-viewer>` routes `audio/*` → native `<audio controls>`. Browser-native codecs (MP3/AAC/OGG/WAV/FLAC/WebM) served directly from `ContentV2`. Non-native codecs (AMR, WMA, 3GPP, SILK, …) → `ContentV2?transcode=webm` (ffmpeg VP9+Opus). Viewer `mediaUrl()` computed selects the URL. |
| `TiffViewer` | `image/tiff` | WPW | WEB_HTML | `ContentV2?format=png` converts TIFF → PNG via JDK `ImageIO` (built-in TIFF reader, JDK 9+). Viewer `imageUrl()` appends `?format=png` for `image/tiff`. Multi-page TIFF shows first page only (ImageIO reads page 0). |
| `EmailViewer` | `message/rfc822`, `message/*` | WPW | WEB_HTML | `'email'` viewer type: headers from item metadata + HTML body from text endpoint via `forkJoin`. CID inline-image rewriting not yet implemented (broken inline images). |
| `MsgViewer` | `application/vnd.ms-outlook` | WPW | WEB_HTML | Routed to `'email'` viewer via `resolveViewerType`. Tika metadata supplies headers; text endpoint supplies HTML body (Apache POI MSG→HTML conversion happens during indexing). CID rewriting pending. |
| `CADViewer` | `image/vnd.dwg`, `image/vnd.dxf`, `model/*` | NO | NATIVE_SWING | Requires proprietary CAD SDK (Teigha / ODA). No browser rendering path. Remains Swing-only for 5.0. |
| `LibreOfficeViewer` | `application/msword`, `application/vnd.ms-excel`, `application/vnd.oasis.*`, etc. | NO | NATIVE_BRIDGE | LibreOffice process embedding is the primary use-case for the companion desktop app. Retirement criterion: companion app delivers signed LibreOffice bridge with ≥95% parity on golden item set. |
| `ReferencedFileViewer` | any (linked file) | NO | NATIVE_BRIDGE | Opens a file via the OS shell from the local file system — requires local privileges and OS integration. Browser cannot do this; companion app is the only path. |

---

## MIME fallback chain

When the framework routes an item, it walks this chain until a viewer accepts it:

```
1. Exact MIME match (e.g. message/rfc822 → EmailViewer)
2. Supertype match  (e.g. image/* → ImageViewer)
3. WEB_PORTABLE catch-all → HexViewerPlus (binary hex fallback)
4. If rendering target is NATIVE_BRIDGE → companion app handoff
5. If rendering target is NATIVE_SWING  → Swing desktop app
```

Items in `WEB_PORTABLE_WITH_WORK` viewers fall back to the hex viewer until their
dedicated web island is complete.

---

## Retirement criteria (NATIVE_ONLY viewers)

| Viewer | Retirement trigger |
|---|---|
| `LibreOfficeViewer` | Companion app bridge delivers LO rendering for DOCX/XLSX/PPTX/ODT with ≥95% visual parity on the golden item set AND companion installer is signed and auto-updated. |
| `ReferencedFileViewer` | Companion app delivers OS-shell file-open with security prompt and audit log entry; linked-file support is documented in the threat model. |
| `CADViewer` | Either: a web-capable CAD renderer (e.g. three-dxf, AutoCAD Web) is integrated as an island, OR a server-side converter (rasterise to PNG) reaches ≥90% format coverage on the golden item set. |

---

## Progress tracking

| Phase | Deliverable | Status |
|---|---|---|
| Phase 1 | Contract types (`RenderTarget`, `ViewerLifecycleEvent`, `ViewerClassification`, `IViewerCapability`, `ViewerCapabilityDescriptor`) | ✅ done |
| Phase 1 | This classification matrix | ✅ done |
| Phase 2 | Priority web viewers (text, HTML, image, PDF, hex) implemented as islands | ✅ done (iped-webui Phase 5) |
| Phase 2 | Audio island + transcode fallback | ✅ done — `<audio>`/`<video>` in `<iped-viewer>`; `ContentV2?transcode=webm` via ffmpeg for non-browser codecs |
| Phase 2 | TIFF conversion endpoint + island | ✅ done — `ContentV2?format=png` + `imageUrl()` computed in viewer |
| Phase 2 | Email/MSG island | ✅ done — `'email'` viewer type with header grid + body via `forkJoin`; CID inline-image rewriting pending |
| Phase 2 | Video viewer | ✅ done — `video/*` → `<video controls>` in `<iped-viewer>` |
| Phase 3 | LibreOffice companion bridge | ⬜ |
| Phase 3 | ReferencedFile companion bridge | ⬜ |
| Phase 4 | CAD viewer retirement assessment | ⬜ |
