# iped-ad1 — Evolution Roadmap

> Module purpose: AccessData AD1 logical-image datasource reader
> (`iped.engine.datasource.ad1`).
> Status legend: `[ ]` planned · `[~]` in progress · `[x]` done

## Current state (2026-06)
- [x] Extracted from the monolithic engine into its own module.
- Allowed dependencies: `iped-engine-core` + `iped-api` only (within the parent).
- Small, self-contained format decoder — the simplest of the datasource modules and the
  template for how the others should look.

## Phase 1 — Boundary and registration
- [ ] ArchUnit rule enforcing the dependency constraint.
- [ ] Register via the `ServiceLoader` datasource SPI when available.

## Phase 2 — Robustness
- [ ] Test fixtures: small AD1 samples (single-segment, multi-segment `.ad1/.ad2...`,
      compressed entries, corrupted tail) with decode regression tests.
- [ ] Graceful handling of truncated segment chains — report missing segments per item
      instead of failing the evidence.
- [ ] Document the AD1 format knowledge embedded in the decoder (header layout, chunk
      compression) in a `FORMAT.md` so it isn't tribal knowledge.

## Phase 3 — Distributed processing
- [ ] AD1 entries are independently addressable — expose entry-range work units for the
      Kafka pipeline (good first distributed datasource because of its simplicity).

## Progress checks
- Fixture decode tests green: `mvn -pl iped-engine-parent/iped-ad1 -am test`.
- Engine processes other evidence types with this module absent.
