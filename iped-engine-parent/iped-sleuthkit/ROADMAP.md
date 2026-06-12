# iped-sleuthkit — Evolution Roadmap

> Module purpose: Sleuthkit-based datasource reader — disk images (E01, raw, VHD, etc.),
> file-system decoding via TSK JNI bindings (`sleuthkit.version` pinned in the root pom).
> Status legend: `[ ]` planned · `[~]` in progress · `[x]` done

## Current state (2026-06)
- [x] Extracted from the monolithic engine into its own module.
- Allowed dependencies: `iped-engine-core` + `iped-api` only (within the parent).

## Phase 1 — Boundary and registration
- [ ] ArchUnit rule enforcing the dependency constraint (no `iped-engine`, no siblings).
- [ ] Register the reader via the `ServiceLoader` datasource SPI (when it lands in
      `iped-api`) instead of hardcoded engine wiring.
- [ ] Isolate JNI/native-lib loading and lifecycle here — the engine must not need to know
      TSK exists; native load failure surfaces as "Sleuthkit datasource unavailable".

## Phase 2 — Robustness
- [ ] Corrupt-image handling test corpus: truncated E01, bad partition tables, exotic
      file systems — reader degrades per-entry, never aborts the whole evidence item.
- [ ] TSK upgrade cadence: track upstream Sleuthkit releases; document the patch set
      carried in `4.12.0.p1` so upgrades don't silently drop fixes.
- [ ] Memory behavior under multi-case load (JNI handles are process-global — verify
      per-case isolation of TSK case databases).

## Phase 3 — Distributed processing
- [ ] Segment-addressable reads: agents must open a sub-range of an image
      independently (image accessible via shared storage; offsets/inode lists as work
      units) for the Kafka pipeline.
- [ ] Read-throughput benchmark to size distributed work units.

## Progress checks
- Module builds and tests in isolation: `mvn -pl iped-engine-parent/iped-sleuthkit -am test`.
- Engine starts and processes non-disk evidence with this module removed from classpath.
