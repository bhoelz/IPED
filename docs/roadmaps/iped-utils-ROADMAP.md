# iped-utils — Evolution Roadmap

> Module purpose: shared low-level utilities (`iped.utils`) used across the whole project:
> IO/stream helpers, string/date handling, image utilities, file system helpers.
> Status legend: see linked issues in docs/issues/ for per-item status (canonical statuses: proposed, planned, in_progress, blocked, done, cancelled).

## Current state (2026-06)
- Single flat package `iped.utils`; a grab-bag accumulated over years.
- Depended on by nearly every module, which makes any change high-blast-radius.

## Phase 1 — Inventory and pruning — `planned`
- [ISSUE-344](../issues/ISSUE-344-inventory-public-classes-usage.md) — Inventory all public classes and map usage per consumer module — `planned`
- [ISSUE-345](../issues/ISSUE-345-delete-dead-utilities.md) — Delete dead utilities with zero usages across the reactor — `planned`
- [ISSUE-346](../issues/ISSUE-346-replace-helpers-with-jdk25-stdlib.md) — Replace hand-rolled helpers with JDK 25 standard library equivalents — `planned`
- [ISSUE-347](../issues/ISSUE-347-move-single-consumer-utilities.md) — Move single-consumer utilities to their owning module — `planned`

## Phase 2 — Structure and quality — `planned`
- [ISSUE-348](../issues/ISSUE-348-split-flat-package-into-subpackages.md) — Split the flat iped.utils package into cohesive subpackages — `planned`
- [ISSUE-349](../issues/ISSUE-349-unit-test-coverage-for-utilities.md) — Bring unit-test coverage to all non-trivial utilities — `planned`
- [ISSUE-350](../issues/ISSUE-350-adopt-slf4j-logging-convention.md) — Adopt the project logging convention for logging utilities — `planned`

## Phase 3 — Long-term shape — `planned`
- [ISSUE-351](../issues/ISSUE-351-decide-utilities-surviving-into-5.0.md) — Decide which utilities survive into 5.0 — `planned`
- [ISSUE-352](../issues/ISSUE-352-zero-module-deps-archunit.md) — Keep iped-utils at zero dependencies on other IPED modules — `planned`

## Constraints
- This module must remain dependency-light; it sits below `iped-api` in the stack.
- Any removal needs a full-reactor compile check — usages hide in scripts and resources too.
