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
- [ ] Finalize the viewer capability contract in `iped-viewers-api`: MIME routing,
      fallback chain, rendering target (web HTML / native bridge), lifecycle events
      (open/loading/ready/error/closed per root roadmap workstream 2).
- [ ] Classify every existing viewer: web-portable, web-portable-with-work, or
      native-only (input: the Phase 0 mapping spec; output: a tracked matrix here).

## Phase 2 — Web viewer buildout (5.0 workstream 1)
- [ ] Priority web viewers: text, HTML (sanitized), image, PDF, basic email — served
      through `iped-webapi` and rendered in the browser UI / SSR islands.
- [ ] Hex viewer and metadata viewer as web components (already prototyped in the SSR
      workspace fragments — promote to real implementations).
- [ ] Media playback (video/audio) with server-side transcode fallback for non-browser
      codecs (reuse existing VLC/ffmpeg integration server-side).
- [ ] Sanitization/security review for rendering hostile evidence content in a browser
      context (CSP, sandboxed iframes, no script execution from evidence).

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
