# iped-carvers — Evolution Roadmap

> Module purpose: data carving — `iped-carvers-api` (contracts), `iped-carvers-impl`
> (signature-based carvers), `iped-ahocorasick` (multi-pattern search engine used for
> signature scanning).
> Status legend: see linked issues in docs/issues/ for per-item status (canonical statuses: proposed, planned, in_progress, blocked, done, cancelled).

## Current state (2026-06)
- Stable three-module layout; carving orchestrated by `iped-tasks-carving`.
- Carver definitions partly code-based, partly config-driven.

## Phase 1 — Hygiene — `done`
- [ISSUE-045](../issues/ISSUE-045-carvers-boundary-archunit.md) — ArchUnit test enforcing carver module boundaries — `done`
- [ISSUE-046](../issues/ISSUE-046-carverconfig-toml-translation.md) — CarverConfig.toml — full TOML translation of CarverConfig.xml — `done`
- [ISSUE-047](../issues/ISSUE-047-tomlcarverconfiguration-impl.md) — TomlCarverConfiguration reads CarverConfig.toml via TomlMapper — `done`
- [ISSUE-048](../issues/ISSUE-048-carvertaskconfig-toml-xml-override.md) — Wire CarverTaskConfig to load TOML first with XML overrides — `done`
- [ISSUE-049](../issues/ISSUE-049-carver-fixture-tests.md) — Fixture tests for SQLite, DER, and EML carvers — `done`

## Phase 2 — Performance — `done`
- [ISSUE-050](../issues/ISSUE-050-ahocorasick-jmh-benchmark.md) — JMH benchmark for AhoCorasick scanning paths — `done`
- [ISSUE-051](../issues/ISSUE-051-zero-copy-scanning-path.md) — Eliminate redundant buffer copy in the carving scan path — `done`

## Phase 3 — Capability growth — `blocked`
- [ISSUE-052](../issues/ISSUE-052-carved-item-validator-hooks.md) — Pluggable validator hooks per carved type — `done`
- [ISSUE-053](../issues/ISSUE-053-carver-plugin-spi-parity.md) — Carver plugin SPI parity with parsers — `done`
- [ISSUE-054](../issues/ISSUE-054-distributed-carving-byte-ranges.md) — Distributed carving — unallocated-space ranges as Kafka work units — `blocked`

## Progress checks
- Fixture suite green; benchmark numbers recorded per release.
- All signatures externalized to TOML (zero hardcoded signature tables).
