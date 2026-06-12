# iped-engine-hashdb — Evolution Roadmap

> Module purpose: hash-database lookups during processing — known-file filtering (NSRL),
> child-abuse hash sets (LedDB), PhotoDNA, and IPED's own hashdb format.
> Status legend: `[ ]` planned · `[~]` in progress · `[x]` done

## Current state (2026-06)
- Extracted from the monolithic engine into its own module.

## Phase 1 — Module boundary
- [ ] Depend only on `iped-engine-core` + `iped-api`; enforce with ArchUnit.
- [ ] Hash-lookup SPI so tasks consume lookups through an interface — enables alternate
      backends and clean unit testing without a real hash DB file.
- [ ] Optional at runtime: missing hashdb config disables lookup tasks gracefully.

## Phase 2 — Performance and formats
- [ ] Benchmark suite for lookup throughput (lookups/sec at 1M, 100M, 1B entries) to
      protect against regressions.
- [ ] Concurrent multi-case access to a shared hash DB: verify read path is lock-free or
      properly shared (one mapped DB serving all CaseContexts, not one copy per case).
- [ ] Import tooling hardening: resumable imports, validation reports for malformed
      source sets.

## Phase 3 — Distributed processing
- [ ] Hash DB distribution strategy for agents: shared network copy vs per-agent local
      replica; document the supported topologies and add config for each.
- [ ] Version/fingerprint the hash DB so distributed agents can detect mismatched copies
      (different DB versions across agents would make results non-reproducible).

## Progress checks
- Lookup benchmark recorded per release; no >10% regression unexplained.
- Multi-case test: two concurrent cases share one DB without contention errors.
