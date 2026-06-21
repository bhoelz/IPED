# ISSUE-409: Viewer-host island (iped-viewer) — MIME-type routing to sub-renderers

- Status: done
- Roadmap: [iped-webui-ROADMAP.md](../roadmaps/iped-webui-ROADMAP.md)
- Roadmap section: Phase 5 — Viewer island and event contract extension
- Owner: unassigned
- Created: 2026-06-21
- Updated: 2026-06-21

## Summary

`ViewerComponent` (`<iped-viewer>`) extends `IslandBase` and routes by MIME type to five rendering modes: text, html, image, pdf, and hex (delegating inline to `HexViewerComponent` without double custom-element wrapping); unknown/binary types default to hex, and unsupported modes get a download link. Dispatches `viewerReadyEvent` on load and forwards `previous-page`/`next-page` commands to the embedded hex viewer.

## Problem

The application needed a single viewer entry point that could route any item to the correct rendering strategy based on its MIME type, without each island reimplementing that logic.

## Acceptance criteria

- [x] Routes text/html/image/pdf/hex by MIME type; unknown/binary defaults to hex.
- [x] Unsupported mode shows a download link.
- [x] Dispatches `viewerReadyEvent({itemId, viewerType, mediaType})` on load.
- [x] Forwards `previous-page`/`next-page` to the embedded hex viewer.
- [x] Registered as `<iped-viewer>` custom element in `main.ts`.

## Updates

### 2026-06-21
- Issue created during roadmap consolidation, extracted from `iped-webui-ROADMAP.md`, status set to `done` based on the original `[x]` marker. This is the substantive implementation forward-referenced from Phase 2 in ISSUE-400.
