# ISSUE-219: Define a Tika upgrade policy gated by the fixture corpus

- Status: planned
- Roadmap: [iped-parsers-ROADMAP.md](../roadmaps/iped-parsers-ROADMAP.md)
- Roadmap section: Phase 2 — Quality and coverage
- Owner: unassigned
- Created: 2026-06-21
- Updated: 2026-06-21

## Summary

A formal policy is needed for tracking upstream Tika releases and deciding when/how
to upgrade, using the per-parser fixture regression corpus (see ISSUE-218) as the
upgrade gate.

## Problem

There is currently no documented process for tracking Tika releases or validating
an upgrade beyond ad hoc testing; the project is on a Tika 3.3 baseline with no
defined cadence for re-evaluating new releases.

## Acceptance criteria

- [ ] Document a process for tracking new Tika releases.
- [ ] Require the full fixture corpus to pass as the gate before adopting a new
      Tika version.

## Updates

### 2026-06-21
- Issue created during roadmap consolidation, extracted from `iped-parsers-ROADMAP.md`, status set to `planned` based on the original `[ ]` marker.
