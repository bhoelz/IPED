# ISSUE-449: Standalone ImageThumbTask test with a real image

- Status: planned
- Roadmap: [iped-tasks-ROADMAP.md](../roadmaps/iped-tasks-ROADMAP.md)
- Roadmap section: Phase 6 — Standalone task runner (iped-tasks-cli) hardening
- Owner: unassigned
- Created: 2026-07-11
- Updated: 2026-07-11

## Summary

`ImageThumbTask` is listed in `TaskCompatibility.COMPATIBLE_SEED` (iped-tasks-cli)
as safe to run with `caseData == null`, but no test ever actually runs it
standalone. A 2026-07-11 code review found `ImageThumbTask.accum()`
dereferenced `caseData` unconditionally, so every standalone invocation would
NPE on the first item — exactly the gap this issue exists to close. The NPE
was fixed (a `standaloneAccum` fallback matching the carve tasks' pattern, plus
guards on three `stats.incTimeouts()` call sites), but the fix itself is only
covered by compilation, not by a test that exercises the standalone path.

## Problem

There is no test that runs `ImageThumbTask` through `TaskExecutor`/
`StandaloneTaskCli` (`caseData == null`, `stats == null`) against a real image
file and asserts a thumbnail is actually produced without throwing. Because
`TaskExecutor.classifyFailure` maps a bare `NullPointerException` to
`ItemResult.UNSUPPORTED` (not `ERROR`), a regression here would silently look
like "task doesn't support standalone mode" in CLI output rather than failing
loudly — see the related, not-yet-fixed classification concern noted in the
same review.

## Impact

`ImageThumbTask` is one of the more commonly-needed standalone tasks (thumbnail
generation for a single file/directory, no case). A silent regression back to
the pre-fix NPE would misclassify every image as unsupported instead of
surfacing a real bug.

## Scope

### In scope
- A small real image fixture (JPEG or PNG) checked into test resources.
- A test that builds an `IItem` via `StandaloneItemFactory`, runs it through `TaskExecutor.run("iped.engine.task.ImageThumbTask", items, confDir)` with `caseData == null`, and asserts: no exception/`ERROR`/`UNSUPPORTED` result, and a thumbnail was actually written (check `evidence.getThumb()` or the configured thumb output path, whichever `ImageThumbTask.process()` populates).
- Covering the `finish()` path too, since it resets `standaloneAccum` — run the task against at least two items in the same `TaskExecutor.run()` call (which reuses one task instance and one accumulator) to confirm shared state across items works before `finish()` clears it.

### Out of scope
- `DocThumbTask`/`VideoThumbTask` (same class of bug is plausible there but needs its own fixtures — file separately if review turns up evidence).
- Performance/timeout behavior of `ExternalImageConverter` (needs a slow/corrupt fixture, different concern).

## Acceptance criteria

- [ ] New test lives under `iped-tasks/iped-tasks-cli/src/test/java/iped/tasks/cli/` (cross-module dependency on `iped-tasks-image` — check `iped-tasks-cli/pom.xml` already has or can add a test-scope dependency on it).
- [ ] Test fails if `ImageThumbTask.accum()`'s `caseData == null` branch is removed.
- [ ] Test fails if the `stats != null` guards around `stats.incTimeouts()` are removed and a timeout path is hit.
- [ ] `mvn -pl iped-tasks/iped-tasks-cli -am test` passes.

## Updates

### 2026-07-11
- Issue created from a code-review follow-up (standalone/no-case task hardening review). The review found and fixed the underlying `accum()` NPE by inspection/compilation only, since building an image fixture + wiring the cross-module test dependency was out of scope for that pass.
