# ISSUE-050: JMH benchmark for AhoCorasick scanning paths

- Status: done
- Roadmap: [iped-carvers-ROADMAP.md](../roadmaps/iped-carvers-ROADMAP.md)
- Roadmap section: Phase 2 — Performance
- Owner: unassigned
- Created: 2026-06-21
- Updated: 2026-06-21

## Summary

Add a JMH benchmark comparing `ahoCorasick()`, `ahoCorasickLengthAware()` (which exercises the zero-copy `continueSearch()` path), and `javaRegex()` on a 1 MB synthetic haystack, replacing the ad-hoc `Benchmark.java` main class with a proper, repeatable benchmark harness.

## Problem

Signature-scanning performance was previously only measured via an ad-hoc `Benchmark.java` main class, with no standardized, repeatable benchmark methodology to track regressions or improvements over time.

## Acceptance criteria

- [x] `AhoCorasickBenchmark` added in `iped-ahocorasick/src/test/java`.
- [x] Compares `ahoCorasick()`, `ahoCorasickLengthAware()`, and `javaRegex()` on a 1 MB synthetic haystack.
- [x] `jmh-core:1.37` and `jmh-generator-annprocess` added at `test` scope.
- [x] Ad-hoc `Benchmark.java` main class replaced.

## Updates

### 2026-06-21
- Issue created during roadmap consolidation, extracted from `iped-carvers-ROADMAP.md`, status set to `done` based on the original `[x]` marker.
