# iped-carvers — Evolution Roadmap

> Module purpose: data carving — `iped-carvers-api` (contracts), `iped-carvers-impl`
> (signature-based carvers), `iped-ahocorasick` (multi-pattern search engine used for
> signature scanning).
> Status legend: `[ ]` planned · `[~]` in progress · `[x]` done

## Current state (2026-06)
- Stable three-module layout; carving orchestrated by `iped-tasks-carving`.
- Carver definitions partly code-based, partly config-driven.

## Phase 1 — Hygiene
- [x] ArchUnit: `CarversBoundaryTest` in `iped-carvers-impl` enforces that no
      carver class imports `iped.engine.*` or `iped.app.*`.
- [x] `CarverConfig.toml` created in `iped-carvers-impl/src/main/resources/iped/config/defaults/conf/`
      — full TOML translation of `CarverConfig.xml` covering all 28 active carver types
      with signatures, size bounds, length-ref offsets, and MIME types.
- [x] `TomlCarverConfiguration` implemented in `iped-tasks-carving` — reads `CarverConfig.toml`
      via `TomlMapper` directly; when no inline sigs are present for a carver entry the named
      class is instantiated to supply its own `CarverType[]`. `XMLCarverConfiguration` fields
      promoted to `protected` so the subclass can extend cleanly.
- [x] `CarverTaskConfig` wired: TOML file is loaded first as the primary source; user-supplied
      `CarverConfig.xml` / `carver-*.xml` files are applied on top as overrides (backward
      compatible with existing case-level XML configuration).
- [x] Fixture tests added: `SQLiteCarverFixtureTest`, `DERCarverFixtureTest`,
      `EMLCarverFixtureTest` covering CarverType metadata, signature decode lengths,
      header/footer counts, and size-bound assertions.

## Phase 2 — Performance
- [x] JMH benchmark added: `AhoCorasickBenchmark` in `iped-ahocorasick/src/test/java`
      compares `ahoCorasick()`, `ahoCorasickLengthAware()` (exercises the zero-copy
      `continueSearch()` path), and `javaRegex()` on a 1 MB synthetic haystack.
      `jmh-core:1.37` + `jmh-generator-annprocess` added at `test` scope.
      Ad-hoc `Benchmark.java` main class replaced.
- [x] Zero-copy scanning path — redundant buffer copy eliminated:
      `CarverTask.fillBuf()` previously copied each 1 MB chunk into a fresh `cBuf`
      array before passing it to `AhoCorasick.continueSearch()`. Fix:
      - `SearchResult` now carries a `length` field (new overload constructor) so
        scanning stops at `length` rather than `bytes.length`
      - `AhoCorasick.continueSearch()` uses `lastResult.length` in both `search` paths
      - `CarverTask` passes the shared `buf` directly with the valid `len`, eliminating
        one `new byte[len]` + `System.arraycopy` per 1 MB chunk scanned

## Phase 3 — Capability growth
- [x] Pluggable validator hooks per carved type: `CarvedItemValidator` interface added to
      `iped-carvers-api`; `CarverType.addValidator()`/`getValidators()` wires them in;
      `AbstractCarver.isValid()` runs all registered validators after the built-in
      `validateCarvedObject()` check — any `false` discards the candidate item.
      Third-party code can attach validators to any `CarverType` at startup without
      subclassing `AbstractCarver`.
- [ ] Carver plugin SPI parity with parsers: third parties can ship a carver plugin jar
      with signatures + validator.
- [ ] Distributed carving: unallocated-space ranges as Kafka work units (coordinate with
      `iped-distributed` work-unit model).

## Progress checks
- Fixture suite green; benchmark numbers recorded per release.
- All signatures externalized to TOML (zero hardcoded signature tables).
