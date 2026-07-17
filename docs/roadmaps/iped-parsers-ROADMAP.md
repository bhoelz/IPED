# iped-parsers — Evolution Roadmap

> Module purpose: parent of all artifact parsers (~31 submodules): Tika extensions and
> forensic-specific parsers (registry, lnk, usnjrnl, browsers, P2P apps, mail, sqlite
> family, mobile chat apps, etc.), plus `iped-parsers-common/main/impl` aggregation.
> Status legend: see linked issues in docs/issues/ for per-item status (canonical statuses: proposed, planned, in_progress, blocked, done, cancelled).

## Current state (2026-06)
- Per-parser modularization already done (one Maven module per parser family); built as
  plugins into the shared `plugins` folder (`plugin.dir` in the root pom).
- `iped-parsers-impl` still aggregates a long tail of parsers not yet split out.
- Tika 3.3 baseline.

## Phase 1 — Finish the per-parser split — `in_progress`
- [ISSUE-208](../issues/ISSUE-208-split-iped-parsers-impl-tail.md) — Inventory and prioritize remaining iped-parsers-impl split candidates — `in_progress`
- [ISSUE-209](../issues/ISSUE-209-move-shared-prereqs-to-parsers-common.md) — Move shared parser-split prerequisites into iped-parsers-common — `done`
- [ISSUE-210](../issues/ISSUE-210-split-iped-parser-whatsapp-module.md) — Split iped-parser-whatsapp out of iped-parsers-impl — `done`
- [ISSUE-211](../issues/ISSUE-211-split-iped-parser-threema-module.md) — Split iped-parser-threema out of iped-parsers-impl — `done`
- [ISSUE-212](../issues/ISSUE-212-split-iped-parser-telegram-module.md) — Split iped-parser-telegram out of iped-parsers-impl — `done`
- [ISSUE-213](../issues/ISSUE-213-split-iped-parser-ufed-module.md) — Split iped-parser-ufed out of iped-parsers-impl — `done`
- [ISSUE-214](../issues/ISSUE-214-split-iped-parser-compression-module.md) — Split iped-parser-compression out of iped-parsers-impl — `done`
- [ISSUE-215](../issues/ISSUE-215-per-module-dependency-hygiene-pass.md) — Per-module dependency hygiene pass across split parser modules — `done`
- [ISSUE-216](../issues/ISSUE-216-parsers-boundary-archunit-guard.md) — Add ParsersBoundaryTest ArchUnit guard to iped-parsers-impl — `done`
- [ISSUE-217](../issues/ISSUE-217-confirm-parser-plugin-spi-contract.md) — Confirm and document the parser plugin SPI contract — `done`

## Phase 2 — Quality and coverage — `in_progress`
- [ISSUE-218](../issues/ISSUE-218-fixture-based-regression-corpus-per-parser.md) — Fixture-based regression corpus per parser module — `in_progress`
- [ISSUE-219](../issues/ISSUE-219-tika-upgrade-policy.md) — Define a Tika upgrade policy gated by the fixture corpus — `planned`
- [ISSUE-220](../issues/ISSUE-220-standardize-metadata-property-naming.md) — Standardize chat-message metadata property naming across parsers — `done`
- [ISSUE-221](../issues/ISSUE-221-slf4j-logging-migration-complete.md) — Confirm @Slf4j logging migration complete across split parser modules — `done`

## Phase 3 — Architecture — `in_progress`
- [ISSUE-222](../issues/ISSUE-222-extend-boundary-archunit-to-all-modules.md) — Extend the no-engine-imports boundary rule to every parser module — `done`
- [ISSUE-223](../issues/ISSUE-223-decouple-html-report-from-swing.md) — Decouple parser HTML report generation from Swing/viewer assumptions — `planned`
- [ISSUE-224](../issues/ISSUE-224-external-process-parser-isolation.md) — Evaluate sandboxing/timeout hardening for external-process parsers — `planned`

## Phase 4 — 5.0 — `planned`
- [ISSUE-225](../issues/ISSUE-225-stateless-parser-execution-for-distributed.md) — Make parser execution compatible with the distributed work-unit model — `planned`
- [ISSUE-226](../issues/ISSUE-226-per-parser-docs-page-generation.md) — Generate a per-parser documentation page into the API docs set — `planned`

## Progress checks
- `mvn -pl iped-parsers -am verify` green; fixture corpus green.
- No parser module imports `iped.engine.*` (ArchUnit).
