# iped-ufed — Evolution Roadmap

> Module purpose: UFED/Cellebrite datasource reader — parses UFDR/UFED XML reports and
> maps extracted mobile artifacts into the IPED item model.
> Status legend: `[ ]` planned · `[~]` in progress · `[x]` done

## Current state (2026-06)
- [x] Extracted from the monolithic engine into its own module
      (`iped.engine.datasource.ufed`).
- Allowed dependencies: `iped-engine-core` + `iped-api` only (within the parent).

## Phase 1 — Boundary and registration
- [ ] ArchUnit rule enforcing the dependency constraint.
- [ ] Register via the `ServiceLoader` datasource SPI when available.
- [ ] Untangle any parser-side UFED coupling: UFED chat/contact rendering living in
      `iped-parsers` should consume a shared artifact model, not reader internals.

## Phase 2 — Format coverage
- [ ] Version matrix of supported UFED/Physical Analyzer export versions with regression
      fixtures per version (UFED XML schema drifts release to release).
- [ ] Streaming XML parse for very large UFDR reports (avoid DOM-loading multi-GB files);
      measure peak memory on a large real report.
- [ ] Unknown-element telemetry: log and count artifact types the mapper doesn't handle,
      so coverage gaps are visible instead of silent.

## Phase 3 — Distributed processing
- [ ] Decide the work-unit story: UFDR sections are sequential XML — either keep UFED
      evidence single-agent or implement a split-by-section pre-pass; document the choice.

## Progress checks
- Fixture suite covering at least the 3 most common UFED export versions, green in CI.
- Engine processes non-UFED evidence with this module absent.
