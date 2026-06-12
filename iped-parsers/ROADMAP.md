# iped-parsers — Evolution Roadmap

> Module purpose: parent of all artifact parsers (~31 submodules): Tika extensions and
> forensic-specific parsers (registry, lnk, usnjrnl, browsers, P2P apps, mail, sqlite
> family, mobile chat apps, etc.), plus `iped-parsers-common/main/impl` aggregation.
> Status legend: `[ ]` planned · `[~]` in progress · `[x]` done

## Current state (2026-06)
- Per-parser modularization already done (one Maven module per parser family); built as
  plugins into the shared `plugins` folder (`plugin.dir` in the root pom).
- `iped-parsers-impl` still aggregates a long tail of parsers not yet split out.
- Tika 3.3 baseline.

## Phase 1 — Finish the per-parser split
- [ ] Inventory what remains inside `iped-parsers-impl` and split the largest/highest-
      churn parsers into their own modules (same pattern as `iped-parser-browsers` etc.).
- [ ] Per-module dependency hygiene: each parser declares only what it uses; JDBC drivers
      at `runtime` scope (established convention); remove zero-usage deps.
- [ ] Define the parser plugin contract formally (manifest, supported MIME types,
      ordering/priority) so third-party parser plugins are feasible without forking.

## Phase 2 — Quality and coverage
- [ ] Fixture-based regression corpus per parser module (small real-world samples, run in
      CI) — protects against Tika upgrades and refactors.
- [ ] Tika upgrade policy: track releases, run the full fixture corpus as the gate.
- [ ] Standardize metadata property naming across parsers (audit drift against
      `iped.properties` definitions in `iped-api`).
- [ ] Logging migration to `@Slf4j` complete across all parser modules.

## Phase 3 — Architecture
- [ ] Parsers consume only `iped-api` + Tika + `iped-parsers-common` — no engine imports
      (ArchUnit rule at the parent level applied to every child).
- [ ] Decouple parser HTML report generation from Swing/viewer assumptions so output
      renders identically in the browser UI (5.0 workstream 1).
- [ ] External-process parser isolation (`iped-parser-external` pattern): evaluate
      sandboxing/timeout hardening for crash-prone native parsers.

## Phase 4 — 5.0
- [ ] Parser execution as distributed work units: parsers must be stateless per-item or
      declare their state requirements (needed by `iped-distributed` work-unit model).
- [ ] Per-parser docs page (what it extracts, properties emitted) generated into the
      scripting/API documentation set.

## Progress checks
- `mvn -pl iped-parsers -am verify` green; fixture corpus green.
- No parser module imports `iped.engine.*` (ArchUnit).
