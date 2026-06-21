# ISSUE-318: Move BaseCarveTask shared carverConfig/ledCarved state into a CarveAccumulator

- Status: done
- Roadmap: [iped-tasks-ROADMAP.md](../roadmaps/iped-tasks-ROADMAP.md)
- Roadmap section: Phase 3 — Distributed and multi-case readiness
- Owner: unassigned
- Created: 2026-06-21
- Updated: 2026-06-21

## Summary

`BaseCarveTask` (`iped-engine`)'s `carverConfig` and `ledCarved` static fields,
shared across `CarverTask`/`LedCarveTask`/`KnownMetCarveTask`, were moved into a
`CarveAccumulator` stored in `caseData`, fixing a bug where case 2 in a batch/report
run could silently reuse case 1's carver config and `ledCarved` offsets.

## Problem

This was the cross-module follow-up flagged in ISSUE-317 (CarverTask's own
case-scoping): the shared base-class state required touching `iped-engine` and all
three carve task subclasses together, which was out of scope for the initial
`CarverTask`-only fix.

## Acceptance criteria

- [x] `carverConfig` and `ledCarved` moved into a `CarveAccumulator` stored in
      `caseData`, accessed via `getCarverConfig()`/`setCarverConfig()`/
      `ledCarved()`, shared correctly across `CarverTask`/`LedCarveTask`/
      `KnownMetCarveTask` via the base class.
- [x] `itensCarved` deliberately left static — confirmed it's read externally (UI
      progress, `Statistics` log) as an intentional cumulative run counter, not
      case data.
- [x] `LedCarveTask`'s own counters (`numCarvedItems`, `bytesHashed`,
      `num512total`, `num512hit`, `finished`) moved into a `LedCarveAccumulator`;
      `ledHashDB`/`hashDBDataSource`/`taskEnabled`/`init` left static (one-time
      hash DB load).
- [x] `KnownMetCarveTask`'s `numCarvedItems`/`finished` moved into a
      `KnownMetAccumulator` the same way.
- [x] `ignoreCorrupted` in `CarverTask` assessed and deliberately left static,
      since `ParsingTask` (a different module/family) reads it directly as
      `CarverTask.ignoreCorrupted` — case-scoping tracked as a known follow-up
      rather than expanding this change's blast radius.

## Updates

### 2026-06-21
- Issue created during roadmap consolidation, extracted from `iped-tasks-ROADMAP.md`, status set to `done` based on the original `[x]` marker.
