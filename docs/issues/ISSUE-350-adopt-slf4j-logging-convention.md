# ISSUE-350: Adopt the project logging convention for logging utilities

- Status: planned
- Roadmap: [iped-utils-ROADMAP.md](../roadmaps/iped-utils-ROADMAP.md)
- Roadmap section: Phase 2 — Structure and quality
- Owner: unassigned
- Created: 2026-06-21
- Updated: 2026-06-21

## Summary

Adopt the project's `@Slf4j` logging convention for any utility class in `iped-utils` that performs logging, aligning the module with the rest of the codebase's Lombok @Slf4j + log4j2 standard.

## Problem

Utilities that log may currently use ad-hoc or inconsistent logging setups instead of the project-wide `@Slf4j` convention, creating inconsistency across the codebase.

## Acceptance criteria

- [ ] Utilities that log identified.
- [ ] Logging in those utilities converted to the `@Slf4j` convention.

## Updates

### 2026-06-21
- Issue created during roadmap consolidation, extracted from `iped-utils-ROADMAP.md`, status set to `planned` based on the original `[ ]` marker.
