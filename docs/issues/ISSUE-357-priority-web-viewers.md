# ISSUE-357: Priority web viewers — text, HTML, image, PDF, basic email

- Status: done
- Roadmap: [iped-viewers-ROADMAP.md](../roadmaps/iped-viewers-ROADMAP.md)
- Roadmap section: Phase 2 — Web viewer buildout (5.0 workstream 1)
- Owner: unassigned
- Created: 2026-06-21
- Updated: 2026-06-21

## Summary

Build the priority set of web viewers — text, HTML (sanitized), image, PDF, and basic email — served through `iped-webapi` and rendered in the browser UI / SSR islands.

## Problem

The browser UI needed concrete viewer implementations for the most common content types before any meaningful portion of case review could move off the Swing desktop client.

## Acceptance criteria

- [x] `<iped-viewer>` island (iped-webui Phase 5) added, MIME-routing to text (`TextV2` as `text/plain`), HTML (`text/html` via `[innerHTML]` with DOMPurify), image (direct `<img>` from `ContentV2`), PDF (`<embed type=pdf>`), and hex (delegates to `<iped-hex-viewer>`).
- [x] Basic email bodies rendered via the HTML/text path; multi-part EML assembly tracked separately as WEB_PORTABLE_WITH_WORK per the classification matrix.

## Updates

### 2026-06-21
- Issue created during roadmap consolidation, extracted from `iped-viewers-ROADMAP.md`, status set to `done` based on the original `[x]` marker.
