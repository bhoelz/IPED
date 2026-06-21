# ISSUE-061: Structured status events with a versioned schema

- Status: done
- Roadmap: [iped-distributed-ROADMAP.md](../roadmaps/iped-distributed-ROADMAP.md)
- Roadmap section: Phase 2 — Operability
- Owner: unassigned
- Created: 2026-06-21
- Updated: 2026-06-21

## Summary

Give `iped.status` topic events a versioned schema with a documented evolution policy, so old and new agents can coexist safely during rolling upgrades.

## Problem

Without an explicit schema version on status events, evolving the event format risked breaking consumers during rolling upgrades or making forward/backward compatibility ambiguous.

## Acceptance criteria

- [x] `ItemStatusEvent` carries a `schemaVersion: int` field (current value `1`; legacy events deserialize to `0`); all factory methods stamp the version.
- [x] `ItemStatusConsumer.validateSchemaVersion()` logs a warning on future versions (forward-compat via `@JsonIgnoreProperties`) and accepts v0 as a pre-versioned equivalent.
- [x] Full field catalogue, JSON examples, topic-config table, and evolution rules documented in `specs/88-distributed-status-events-schema.md`.

## Updates

### 2026-06-21
- Issue created during roadmap consolidation, extracted from `iped-distributed-ROADMAP.md` (Phase 2), status set to `done`.
