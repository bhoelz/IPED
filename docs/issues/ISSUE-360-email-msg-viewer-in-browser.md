# ISSUE-360: Email/MSG viewer in browser

- Status: done
- Roadmap: [iped-viewers-ROADMAP.md](../roadmaps/iped-viewers-ROADMAP.md)
- Roadmap section: Phase 2 — Web viewer buildout (5.0 workstream 1)
- Owner: unassigned
- Created: 2026-06-21
- Updated: 2026-06-21

## Summary

Add a browser-rendered email/MSG viewer that displays parsed headers alongside the rendered HTML body, covering RFC822/EMLX/MSG/Outlook MIME types.

## Problem

Email and MSG items need a structured header display (Subject/From/To/CC/BCC/Date) plus the rendered body, which the generic text/HTML viewer path doesn't provide on its own.

## Acceptance criteria

- [x] `message/rfc822`, `message/x-emlx`, `message/*`, `application/vnd.ms-outlook` routed to the `'email'` viewer type.
- [x] Two parallel requests via `forkJoin`: item metadata for headers (Subject/From/To/CC/BCC/Date from Tika metadata keys) and text endpoint (`Accept: text/html`) for the rendered body.
- [x] Header grid + separator + `[innerHTML]` body rendered.
- [x] CID inline-image rewriting documented as a follow-up (not yet implemented).

## Updates

### 2026-06-21
- Issue created during roadmap consolidation, extracted from `iped-viewers-ROADMAP.md`, status set to `done` based on the original `[x]` marker.
