# ISSUE-450: Narrow TaskExecutor's NPE-to-UNSUPPORTED classification, add regression test

- Status: planned
- Roadmap: [iped-tasks-ROADMAP.md](../roadmaps/iped-tasks-ROADMAP.md)
- Roadmap section: Phase 6 — Standalone task runner (iped-tasks-cli) hardening
- Owner: unassigned
- Created: 2026-07-11
- Updated: 2026-07-11

## Summary

`TaskExecutor.classifyFailure` (iped-tasks-cli) maps any root-cause
`NullPointerException` thrown while processing an item to
`ItemResult.UNSUPPORTED` with the message "task appears to require case/index
state unavailable standalone". That's a reasonable default for
`TaskCompatibility.Status.UNCLEAR` tasks (not reviewed, or genuinely
conditional), but it also silently swallows real bugs in tasks already
classified `COMPATIBLE` — a 2026-07-11 review found exactly this: an NPE in
`ImageThumbTask.accum()` (a `COMPATIBLE`-listed task) would have been reported
as "unsupported" rather than as the actual defect it was.

## Problem

`classifyFailure` has no access to the task's `TaskCompatibility.Classification`
at the point it runs (it only sees the `Throwable`), so it can't currently
distinguish "an `UNCLEAR` task hit case-state it needs" from "a `COMPATIBLE`
task has a real bug." Both look identical: an NPE.

## Impact

For `COMPATIBLE`-classified tasks, misclassifying a real NPE as `UNSUPPORTED`
hides genuine defects behind a message that tells the caller/CI to stop
investigating ("this task doesn't support standalone mode") rather than
"this task standalone integration is broken, fix it."

## Scope

### In scope
- Thread the task's `TaskCompatibility.Classification` (or just its `Status`) into `TaskExecutor.run()`'s per-item loop so `classifyFailure` can consult it.
- For `Status.COMPATIBLE` tasks: report a root-cause NPE as `ItemResult.ERROR`, not `UNSUPPORTED`.
- For `Status.UNCLEAR`/`Status.KNOWN_UNSUPPORTED` tasks: keep the current NPE → `UNSUPPORTED` behavior (it's a genuinely useful heuristic there).
- A `TaskExecutorTest` case asserting a `COMPATIBLE`-classified task that throws NPE mid-`process()` comes back `ERROR`, and (for contrast, same test or a sibling) an `UNCLEAR` task's NPE still comes back `UNSUPPORTED`.

### Out of scope
- Changing `TaskCompatibility`'s classification of any specific task.
- Non-NPE `RuntimeException` classification (already `ERROR`, not part of this gap).

## Acceptance criteria

- [ ] `classifyFailure` (or its caller) takes the task's compatibility status into account.
- [ ] New `TaskExecutorTest` case covers both branches (COMPATIBLE-task NPE → ERROR; UNCLEAR-task NPE → UNSUPPORTED).
- [ ] Existing `TaskExecutorTest`/`TaskCompatibilityTest` cases still pass unmodified (or are updated with a documented reason if the classification wording changes).
- [ ] `mvn -pl iped-tasks/iped-tasks-cli -am test` passes.

## Updates

### 2026-07-11
- Issue created from a code-review follow-up (standalone/no-case task hardening review). The review flagged the classification gap but did not change `TaskExecutor` itself, since narrowing it needs a design call on whether `Status` should flow into `TaskExecutor.run()`'s signature or be looked up internally.
