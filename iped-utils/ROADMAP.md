# iped-utils — Evolution Roadmap

> Module purpose: shared low-level utilities (`iped.utils`) used across the whole project:
> IO/stream helpers, string/date handling, image utilities, file system helpers.
> Status legend: `[ ]` planned · `[~]` in progress · `[x]` done

## Current state (2026-06)
- Single flat package `iped.utils`; a grab-bag accumulated over years.
- Depended on by nearly every module, which makes any change high-blast-radius.

## Phase 1 — Inventory and pruning
- [ ] Inventory all public classes and map actual usage per consumer module.
- [ ] Delete dead utilities (zero usages across the reactor) — follow the established
      dependency-cleanup convention: remove rather than keep "just in case".
- [ ] Replace hand-rolled helpers that now exist in the JDK 25 standard library
      (e.g., file/stream/string operations) and deprecate the local versions.
- [ ] Identify utilities that belong to a single consumer module and move them there
      (utilities used only by parsers → `iped-parsers-common`, etc.).

## Phase 2 — Structure and quality
- [ ] Split the flat package into cohesive subpackages (`io`, `text`, `image`, `fs`,
      `concurrent`) without breaking binary compatibility in 4.x (deprecated forwarders).
- [ ] Bring unit-test coverage to all non-trivial utilities (they are the most-reused and
      least-tested code in the project).
- [ ] Adopt the project logging convention (`@Slf4j`) for any utility that logs.

## Phase 3 — Long-term shape
- [ ] Decide what survives into 5.0: utilities are a cost, not an asset — prefer well-known
      libraries (commons-io, Guava-equivalents already on the classpath via Tika) over
      bespoke code where behavior is identical.
- [ ] Keep zero dependencies on other IPED modules (enforce with ArchUnit).

## Constraints
- This module must remain dependency-light; it sits below `iped-api` in the stack.
- Any removal needs a full-reactor compile check — usages hide in scripts and resources too.
