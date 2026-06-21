# ISSUE-359: Media playback with server-side transcode fallback

- Status: done
- Roadmap: [iped-viewers-ROADMAP.md](../roadmaps/iped-viewers-ROADMAP.md)
- Roadmap section: Phase 2 — Web viewer buildout (5.0 workstream 1)
- Owner: unassigned
- Created: 2026-06-21
- Updated: 2026-06-21

## Summary

Add video/audio playback in the browser viewer, using browser-native codecs directly where possible and falling back to server-side transcoding for codecs the browser cannot play natively.

## Problem

Many evidence media files use codecs (e.g. AMR, WMA, 3GPP, SILK, AVI, WMV, MOV) that browsers cannot play natively, so direct `<audio>`/`<video>` playback alone would leave a large fraction of media items unviewable in the browser.

## Acceptance criteria

- [x] `audio/*` routed to native `<audio controls>` from `ContentV2` for browser-native codecs (MP3, AAC, OGG, WAV, FLAC, WebM audio); transcription/confidence/duration fetched from item metadata.
- [x] `video/*` routed to native `<video controls>` from `ContentV2` for browser-native codecs (MP4/H.264, WebM/VP8/VP9, OGG/Theora).
- [x] Server-side fallback via `ContentV2?transcode=webm` shells out to ffmpeg (audio → libopus/WebM; video → VP9+Opus/WebM).
- [x] Viewer `mediaUrl()` appends `?transcode=webm` for non-browser-native MIME types.
- [x] Returns 501 when ffmpeg is not on PATH; ffmpeg feeder runs as a daemon thread to avoid blocking the Jersey worker.

## Updates

### 2026-06-21
- Issue created during roadmap consolidation, extracted from `iped-viewers-ROADMAP.md`, status set to `done` based on the original `[x]` marker.
