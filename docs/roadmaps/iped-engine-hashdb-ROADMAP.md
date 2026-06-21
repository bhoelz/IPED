# iped-engine-hashdb — Evolution Roadmap

> Module purpose: hash-database lookups during processing — known-file filtering (NSRL),
> child-abuse hash sets (LedDB), PhotoDNA, and IPED's own hashdb format.
> Status legend: see linked issues in docs/issues/ for per-item status (canonical statuses: proposed, planned, in_progress, blocked, done, cancelled).

## Current state (2026-06)
- Extracted from the monolithic engine into its own module.

## Phase 1 — Module boundary — `planned`
- [ISSUE-118](../issues/ISSUE-118-hashdb-archunit-boundary.md) — ArchUnit rule for dependency boundary — `planned`
- [ISSUE-119](../issues/ISSUE-119-hashdb-lookup-spi.md) — Hash-lookup SPI for task consumption — `planned`
- [ISSUE-120](../issues/ISSUE-120-hashdb-optional-runtime-graceful-disable.md) — Graceful disable of lookup tasks when config missing — `planned`

## Phase 2 — Performance and formats — `planned`
- [ISSUE-121](../issues/ISSUE-121-hashdb-lookup-benchmark-suite.md) — Benchmark suite for lookup throughput — `planned`
- [ISSUE-122](../issues/ISSUE-122-hashdb-concurrent-multicase-access.md) — Verify concurrent multi-case access to a shared hash DB — `planned`
- [ISSUE-123](../issues/ISSUE-123-hashdb-import-tooling-hardening.md) — Harden import tooling with resumable imports and validation — `planned`

## Phase 3 — Distributed processing — `planned`
- [ISSUE-124](../issues/ISSUE-124-hashdb-distribution-strategy-agents.md) — Hash DB distribution strategy for distributed agents — `planned`
- [ISSUE-125](../issues/ISSUE-125-hashdb-versioning-fingerprint-mismatch-detection.md) — Version/fingerprint hash DB for mismatch detection — `planned`

## Progress checks
- Lookup benchmark recorded per release; no >10% regression unexplained.
- Multi-case test: two concurrent cases share one DB without contention errors.
