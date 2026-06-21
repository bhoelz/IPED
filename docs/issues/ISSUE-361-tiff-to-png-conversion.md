# ISSUE-361: TIFF to PNG server-side conversion for browser rendering

- Status: done
- Roadmap: [iped-viewers-ROADMAP.md](../roadmaps/iped-viewers-ROADMAP.md)
- Roadmap section: Phase 2 — Web viewer buildout (5.0 workstream 1)
- Owner: unassigned
- Created: 2026-06-21
- Updated: 2026-06-21

## Summary

Add server-side TIFF-to-PNG conversion so TIFF images, which browsers cannot render natively, can still be displayed in the browser image viewer.

## Problem

Browsers do not natively render TIFF images, so without server-side conversion, TIFF items would have no usable in-browser image preview.

## Acceptance criteria

- [x] `ContentV2` extended with `?format=png`: uses `ImageIO.read()` then `ImageIO.write(png)` via the JDK 9+ built-in TIFF reader (`com.sun.imageio.plugins.tiff`).
- [x] Returns 422 for non-image content.
- [x] Viewer island `imageUrl()` appends `?format=png` for `image/tiff` / `image/x-tiff`; other image types use the raw `contentUrl()`.

## Updates

### 2026-06-21
- Issue created during roadmap consolidation, extracted from `iped-viewers-ROADMAP.md`, status set to `done` based on the original `[x]` marker.
