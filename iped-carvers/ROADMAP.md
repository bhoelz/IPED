# iped-carvers — Evolution Roadmap

> Module purpose: data carving — `iped-carvers-api` (contracts), `iped-carvers-impl`
> (signature-based carvers), `iped-ahocorasick` (multi-pattern search engine used for
> signature scanning).
> Status legend: `[ ]` planned · `[~]` in progress · `[x]` done

## Current state (2026-06)
- Stable three-module layout; carving orchestrated by `iped-tasks-carving`.
- Carver definitions partly code-based, partly config-driven.

## Phase 1 — Hygiene
- [ ] ArchUnit: carvers depend only on `iped-api` + `iped-carvers-api` (+utils); no
      engine imports.
- [ ] Move all carver signature definitions to the TOML config model (module-owned
      defaults, profile deviations) — one declarative format for signatures, sizes,
      validation rules.
- [ ] Unit fixtures per carver type: synthetic buffers with planted artifacts at
      boundaries (start/end of buffer, overlapping signatures, false-positive bait).

## Phase 2 — Performance
- [ ] Benchmark `iped-ahocorasick` against modern alternatives (e.g., vectorized
      matchers) on representative images; keep or replace based on data.
- [ ] Zero-copy scanning path: verify the carving task streams without redundant buffer
      copies under the multi-case memory quotas.

## Phase 3 — Capability growth
- [ ] Pluggable validator hooks per carved type (cheap header check → optional deep
      validation) to cut false positives on fragmented media.
- [ ] Carver plugin SPI parity with parsers: third parties can ship a carver plugin jar
      with signatures + validator.
- [ ] Distributed carving: unallocated-space ranges as Kafka work units (coordinate with
      `iped-distributed` work-unit model).

## Progress checks
- Fixture suite green; benchmark numbers recorded per release.
- All signatures externalized to TOML (zero hardcoded signature tables).
