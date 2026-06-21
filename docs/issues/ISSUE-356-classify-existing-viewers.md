# ISSUE-356: Classify every existing viewer as web-portable or native-only

- Status: done
- Roadmap: [iped-viewers-ROADMAP.md](../roadmaps/iped-viewers-ROADMAP.md)
- Roadmap section: Phase 1 — Contract-first viewer model
- Owner: unassigned
- Created: 2026-06-21
- Updated: 2026-06-21

## Summary

Classify every existing viewer as web-portable, web-portable-with-work, or native-only, using the Phase 0 mapping spec as input and producing a tracked classification matrix as output.

## Problem

Before any viewer can be ported to the web or bridged to the companion app, each existing Swing viewer needs a clear classification of how much work (if any) is needed to make it web-renderable.

## Acceptance criteria

- [x] Classification matrix written to `specs/95-viewer-classification-matrix.md`.
- [x] WEB_PORTABLE viewers identified: ATextViewer, HtmlViewer, HtmlLinkViewer, ImageViewer, IcePDFViewer, PDFBoxViewer, HexViewerPlus, MetadataViewer.
- [x] WEB_PORTABLE_WITH_WORK viewers identified: AudioViewer (codec/transcode), TiffViewer (server-side conversion), EmailViewer (EML→HTML), MsgViewer (MSG→HTML + CID rewriting).
- [x] NATIVE_ONLY viewers identified: CADViewer (proprietary SDK), LibreOfficeViewer (companion bridge target), ReferencedFileViewer (OS shell, companion bridge).
- [x] MIME fallback chain and retirement criteria documented.

## Updates

### 2026-06-21
- Issue created during roadmap consolidation, extracted from `iped-viewers-ROADMAP.md`, status set to `done` based on the original `[x]` marker.
