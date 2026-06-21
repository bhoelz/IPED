# ISSUE-427: CSP and security headers tuned for evidence-content rendering

- Status: done
- Roadmap: [iped-webui-server-ROADMAP.md](../roadmaps/iped-webui-server-ROADMAP.md)
- Roadmap section: Phase 3 — Auth, sessions, hardening
- Owner: unassigned
- Created: 2026-06-21
- Updated: 2026-06-21

## Summary

`SecurityConfig` was extended with `contentSecurityPolicy()`, `httpStrictTransportSecurity()`, `referrerPolicy(SAME_ORIGIN)`, `frameOptions(DENY)`, `xssProtection(ENABLED_MODE_BLOCK)`, and `permissionsPolicy()`; CSP allows `frame-src 'self'` for HTML rendition and `object-src 'self'` for PDF embed, plus `style-src`/`font-src` for Google Fonts used on login/case-picker pages.

## Problem

Rendering arbitrary evidence content (HTML renditions, PDFs) in-browser needed a carefully scoped CSP so that malicious evidence content couldn't exfiltrate data or execute unintended scripts.

## Acceptance criteria

- [x] CSP, HSTS, referrer policy, frame options, XSS protection, and permissions policy configured.
- [x] CSP allows `frame-src 'self'` (HTML viewer) and `object-src 'self'` (PDF embed) without being permissive elsewhere.

## Updates

### 2026-06-21
- Issue created during roadmap consolidation, extracted from `iped-webui-server-ROADMAP.md`, status set to `done` based on the original `[x]` marker.
