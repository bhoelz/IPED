# ISSUE-133: Make datasource modules optional at runtime with graceful degradation

- Status: planned
- Roadmap: [iped-engine-parent-ROADMAP.md](../roadmaps/iped-engine-parent-ROADMAP.md)
- Roadmap section: Phase 2 — Service-loader datasource registration
- Owner: unassigned
- Created: 2026-06-21
- Updated: 2026-06-21

## Summary

Make datasource modules optional at runtime so an IPED distribution without `iped-sleuthkit` on the classpath degrades gracefully (clear error, not NPE).

## Problem

Today, the absence of a datasource module on the classpath likely produces an unhandled NPE or unclear failure rather than an actionable error message, which would block lean/customized distributions.

## Acceptance criteria

- [ ] Build/run an IPED distribution without `iped-sleuthkit` on the classpath.
- [ ] Confirm a clear, actionable error message is produced instead of an NPE.
- [ ] Repeat for `iped-ufed` and `iped-ad1` absence scenarios.

## Updates

### 2026-06-21
- Issue created during roadmap consolidation, extracted from `iped-engine-parent-ROADMAP.md`, status set to `planned`.
