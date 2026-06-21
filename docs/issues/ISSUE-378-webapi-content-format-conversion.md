# ISSUE-378: On-the-fly content format conversion (PNG, WebM transcode)

- Status: done
- Roadmap: [iped-webapi-ROADMAP.md](../roadmaps/iped-webapi-ROADMAP.md)
- Roadmap section: Phase 4 — Platform (5.0)
- Owner: unassigned
- Created: 2026-06-21
- Updated: 2026-06-21

## Summary

Support on-the-fly format conversion in `ContentV2` so browsers can render content types they don't natively support, via JDK ImageIO for images and ffmpeg subprocess transcoding for audio/video.

## Problem

Some evidence formats (e.g., TIFF images, arbitrary audio/video codecs) are not natively renderable in a browser, blocking the browser UI's viewer parity goals.

## Acceptance criteria

- [x] `?format=png` — JDK `ImageIO` TIFF→PNG conversion (built-in reader, JDK 9+); returns 422 for non-decodable content.
- [x] `?transcode=webm` — ffmpeg server-side transcode: audio → libopus/WebM, video → VP9+Opus/WebM, run as a subprocess with stdin/stdout pipes and a feeder daemon thread; returns 501 when ffmpeg is not on PATH.

## Updates

### 2026-06-21
- Issue created during roadmap consolidation, extracted from `iped-webapi-ROADMAP.md`, status set to `done` based on the original `[x]` marker.
