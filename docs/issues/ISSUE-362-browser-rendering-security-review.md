# ISSUE-362: Sanitization/security review for rendering evidence in the browser

- Status: done
- Roadmap: [iped-viewers-ROADMAP.md](../roadmaps/iped-viewers-ROADMAP.md)
- Roadmap section: Phase 2 — Web viewer buildout (5.0 workstream 1)
- Owner: unassigned
- Created: 2026-06-21
- Updated: 2026-06-21

## Summary

Perform a sanitization/security review covering the risk of rendering hostile evidence content in a browser context, addressing CSP, sandboxed iframes, and prevention of script execution from evidence content.

## Problem

Rendering forensic evidence (which is by definition untrusted, potentially hostile content) directly in a browser viewer creates real script-execution and exfiltration risk if not explicitly sandboxed and policy-restricted.

## Acceptance criteria

- [x] CSP policy added in iped-webui-server Phase 3: `frame-src 'self'` for sandboxed HTML renditions, `object-src 'self'` for PDF `<embed>`, `'unsafe-inline'` styles only for Rocker SSR, `data:`/`blob:` allowed for gallery thumbnails.
- [x] `SecurityConfig` sets `X-Frame-Options: DENY`, `X-XSS-Protection`, Referrer-Policy, and Permissions-Policy.
- [x] `<iped-evidence>` guard added in iped-mcp to prevent prompt injection for AI paths.

## Updates

### 2026-06-21
- Issue created during roadmap consolidation, extracted from `iped-viewers-ROADMAP.md`, status set to `done` based on the original `[x]` marker.
