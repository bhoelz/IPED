# ISSUE-448: End-to-end standalone CarverTask test through the CLI

- Status: done
- Roadmap: [iped-tasks-ROADMAP.md](../roadmaps/iped-tasks-ROADMAP.md)
- Roadmap section: Phase 6 — Standalone task runner (iped-tasks-cli) hardening
- Owner: unassigned
- Created: 2026-07-11
- Updated: 2026-07-11

## Summary

`iped-tasks-cli` lets a single `AbstractTask` run standalone (no case, no
Lucene index, no SQLite) via `StandaloneTaskCli`/`TaskExecutor`. The carving
family is a special case: with `--extract-children-to <dir>`, `TaskExecutor`
routes carved children through `CarvingOutputWorker` instead of failing as
UNSUPPORTED. The only existing coverage
(`CarvingOutputWorkerTest.writesCarvedChildContentAndMetadataToOutputDirectory`)
calls `CarvingOutputWorker.processNewItem()` directly against a synthetic
`IItem` — it never exercises `CarverTask` itself, so it doesn't verify that
signature-based carving detection actually fires standalone.

## Problem

There is no test that drives the real path: `StandaloneTaskCli`/`TaskExecutor`
running `iped.engine.task.carver.CarverTask` against a file containing a real
carvable signature (e.g. an embedded JPEG or ZIP signature inside a larger
blob), with `--extract-children-to` set, and asserting the carved child
actually lands in the output directory with correct content and offset
metadata. Without it, a regression in the standalone accumulator wiring
(`CarverTask.accum()`'s `caseData == null` fallback, `BaseCarveTask`'s
matching fallback, or `CarvingOutputWorker` itself) could pass all existing
tests while the real CLI invocation fails or silently carves nothing.

## Impact

Anyone relying on `--extract-children-to` for standalone carving has no
regression safety net for the part of the pipeline that actually matters
(signature detection → carve → write). This is the CLI's most complex
standalone code path (see `TaskCompatibility.CARVING_FAMILY` and its
`CARVING_NOTE`) and currently the least tested.

## Scope

### In scope
- A fixture file with at least one embedded carvable signature recognized by the default carver config (check `CarverConfig.toml`/`iped-carvers` for a signature that's cheap to embed, e.g. a small JPEG or ZIP).
- A test that calls `TaskExecutor.run("iped.engine.task.carver.CarverTask", items, confDir, childrenDir)` (or goes through `StandaloneTaskCli.main` for a true end-to-end run) and asserts the carved child file(s) and `.metadata.txt` sidecars appear under `childrenDir` with the expected content/offset.
- Asserting the *parent* item's `ItemResult` still comes back `ok`, not `UNSUPPORTED`/`ERROR`.

### Out of scope
- Recursive carving of carved children (explicitly unsupported per `CarvingOutputWorker`'s javadoc — one level only).
- `LedCarveTask`/`KnownMetCarveTask` (hash-DB-driven carving needs a populated hash database fixture; track separately if needed).

## Acceptance criteria

- [ ] New test lives under `iped-tasks/iped-tasks-cli/src/test/java/iped/tasks/cli/` (or `TaskExecutorTest` if that's a better fit).
- [ ] Test fails if `CarverTask.accum()`'s standalone fallback is reverted to unconditionally read `caseData`.
- [ ] Test fails if `CarvingOutputWorker` stops being invoked for the carving family.
- [ ] `mvn -pl iped-tasks/iped-tasks-cli -am test` passes.

## Updates

### 2026-07-11
- Issue created from a code-review follow-up (standalone/no-case task hardening review): flagged as a recommended test that needs a real carvable-signature fixture, which the review itself didn't have time to build.
