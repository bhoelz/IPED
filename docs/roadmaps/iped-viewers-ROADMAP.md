# iped-viewers — Evolution Roadmap

> Module purpose: content viewers — `iped-viewers-api` (viewer contracts),
> `iped-viewers-impl` (Swing/native viewers incl. LibreOffice embedding),
> `iped-viewers-web` (web-renderable viewer output).
> Status legend: see linked issues in docs/issues/ for per-item status (canonical statuses: proposed, planned, in_progress, blocked, done, cancelled).

## Current state (2026-06)
- Swing-first viewer set in `iped-viewers-impl`; web mapping inventoried in
  `specs/83-phase-0-viewers-api-web-mapping.md` (Phase 0 of the rewrite spec).
- `iped-viewers-web` exists as the target for browser-rendered viewing.

## Phase 1 — Contract-first viewer model — `done`
- [ISSUE-355](../issues/ISSUE-355-viewer-capability-contract.md) — Finalize the viewer capability contract in iped-viewers-api — `done`
- [ISSUE-356](../issues/ISSUE-356-classify-existing-viewers.md) — Classify every existing viewer as web-portable or native-only — `done`

## Phase 2 — Web viewer buildout (5.0 workstream 1) — `done`
- [ISSUE-357](../issues/ISSUE-357-priority-web-viewers.md) — Priority web viewers — text, HTML, image, PDF, basic email — `done`
- [ISSUE-358](../issues/ISSUE-358-hex-metadata-viewer-web-components.md) — Hex viewer and metadata viewer as production web components — `done`
- [ISSUE-359](../issues/ISSUE-359-media-playback-with-transcode-fallback.md) — Media playback with server-side transcode fallback — `done`
- [ISSUE-360](../issues/ISSUE-360-email-msg-viewer-in-browser.md) — Email/MSG viewer in browser — `done`
- [ISSUE-361](../issues/ISSUE-361-tiff-to-png-conversion.md) — TIFF to PNG server-side conversion for browser rendering — `done`
- [ISSUE-362](../issues/ISSUE-362-browser-rendering-security-review.md) — Sanitization/security review for rendering evidence in the browser — `done`

## Phase 3 — Companion app bridge (5.0 workstream 2) — `planned`
- [ISSUE-363](../issues/ISSUE-363-native-viewers-companion-bridge.md) — Expose native-only viewers through the companion app bridge — `planned`
- [ISSUE-364](../issues/ISSUE-364-keep-swing-viewers-until-parity.md) — Keep Swing viewer implementations functional until parity validated — `planned`

## Phase 4 — Decommission path — `planned`
- [ISSUE-365](../issues/ISSUE-365-viewer-decommission-checklist.md) — Per-viewer parity sign-off checklist and Swing-only viewer removal — `planned`

## Progress checks
- Viewer matrix in this file updated as classifications/parities land.
- Browser UI renders the priority-five viewer set against a real case.
