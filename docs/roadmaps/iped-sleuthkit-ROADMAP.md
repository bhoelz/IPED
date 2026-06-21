# iped-sleuthkit — Evolution Roadmap

> Module purpose: Sleuthkit-based datasource reader — disk images (E01, raw, VHD, etc.),
> file-system decoding via TSK JNI bindings (`sleuthkit.version` pinned in the root pom).
> Status legend: see linked issues in docs/issues/ for per-item status (canonical statuses: proposed, planned, in_progress, blocked, done, cancelled).

## Current state (2026-06)
- [x] Extracted from the monolithic engine into its own module.
- Allowed dependencies: `iped-engine-core` + `iped-api` only (within the parent).

## Phase 1 — Boundary and registration — `planned`
- [ISSUE-291](../issues/ISSUE-291-sleuthkit-archunit-boundary.md) — ArchUnit rule enforcing the dependency constraint — `planned`
- [ISSUE-292](../issues/ISSUE-292-sleuthkit-serviceloader-registration.md) — Register the reader via the ServiceLoader datasource SPI — `planned`
- [ISSUE-293](../issues/ISSUE-293-sleuthkit-jni-isolation-lifecycle.md) — Isolate JNI/native-lib loading and lifecycle — `planned`

## Phase 2 — Robustness — `planned`
- [ISSUE-294](../issues/ISSUE-294-sleuthkit-corrupt-image-test-corpus.md) — Corrupt-image handling test corpus — `planned`
- [ISSUE-295](../issues/ISSUE-295-sleuthkit-tsk-upgrade-cadence-tracking.md) — Track TSK upgrade cadence and document patch set — `planned`
- [ISSUE-296](../issues/ISSUE-296-sleuthkit-multicase-jni-memory-isolation.md) — Verify per-case TSK isolation under multi-case load — `planned`

## Phase 3 — Distributed processing — `planned`
- [ISSUE-297](../issues/ISSUE-297-sleuthkit-segment-addressable-reads-kafka.md) — Segment-addressable reads for the Kafka pipeline — `planned`
- [ISSUE-298](../issues/ISSUE-298-sleuthkit-read-throughput-benchmark.md) — Read-throughput benchmark to size work units — `planned`

## Progress checks
- Module builds and tests in isolation: `mvn -pl iped-engine-parent/iped-sleuthkit -am test`.
- Engine starts and processes non-disk evidence with this module removed from classpath.
