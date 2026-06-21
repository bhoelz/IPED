# ISSUE-164: Remaining configurable schemas and automated schema generation tooling

- Status: planned
- Roadmap: [iped-engine-schemas-ROADMAP.md](../roadmaps/iped-engine-schemas-ROADMAP.md)
- Roadmap section: Phase 2 — Schema Completion & Automation
- Owner: unassigned
- Created: 2026-06-21
- Updated: 2026-06-21

## Summary

Generate JSON/UI schemas for the remaining 14 configurable components, and build reflection-based tooling (`SchemaExtractor`, `FieldAnalyzer`) plus a CLI validator to automate schema generation and checking going forward.

## Problem

Schema coverage is currently 31 of ~45 configurable components; the remaining 14 (`SplitLargeBinaryConfig`, `LocaleConfig`, `DefaultTaskPropertiesConfig`, `ExportByKeywordsConfig`, `HashDBLookupConfig`, `PhotoDNALookupConfig`, and 8 more) still need schemas. Hand-authoring schemas doesn't scale, so an automated extraction/generation pipeline plus a CLI validation tool is needed.

## Acceptance criteria

- [ ] Generate JSON and UI schemas for the remaining 14 configurable components.
- [ ] Create `SchemaExtractor` using reflection to pull property metadata from Java classes.
- [ ] Parse Javadoc comments for descriptions; detect field types and annotations.
- [ ] Auto-generate JSON schemas and UI schemas with intelligent widget selection.
- [ ] Create `SchemaValidationCLI` to validate all schemas against the JSON Schema meta-schema, test against example configs, generate a validation report, and integrate with CI/CD.

## Updates

### 2026-06-21
- Issue created during roadmap consolidation, extracted from `iped-engine-schemas-ROADMAP.md` (Phase 2 — Schema Completion & Automation, sections 2.1-2.3). Status set to `planned`.
