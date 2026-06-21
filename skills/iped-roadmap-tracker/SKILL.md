---
name: iped-roadmap-tracker
description: Maintain IPED's planning docs — docs/roadmaps/<module>-ROADMAP.md per module, docs/issues/ISSUE-NNN-*.md for PR-sized work items, and the root STATE-OF-IPED.md rollup report. Use this whenever working on IPED's own roadmaps or issues, e.g. "update the iped-engine roadmap", "create an issue for this in IPED's tracker", "what's the status of iped-distributed", "regenerate the state of IPED report", "this module needs a roadmap/README", or when a module's ROADMAP.md/README.md/checkbox list needs to follow the conventions established in the 2026-06 planning consolidation. Builds on the general issue-tracker skill's conventions, specialized to this repo's exact paths and module list.
---

# IPED Roadmap Tracker

IPED's planning docs were consolidated (2026-06) into one consistent structure: every module has a roadmap under `docs/roadmaps/`, every roadmap phase links to PR-sized issues under `docs/issues/`, every module's `README.md` points at its roadmap, and a generated `STATE-OF-IPED.md` rolls everything up. This skill is the IPED-specific specialization of the general `issue-tracker` skill — read that one too if it's available; this one only covers what's particular to this repo.

## Where things live

```
ROADMAP.md                          # master roadmap, project root, cross-cutting workstreams
STATE-OF-IPED.md                    # generated rollup report, project root
docs/
  roadmaps/
    README.md                       # index of all roadmaps
    <module>-ROADMAP.md             # one per module/submodule, e.g. iped-ad1-ROADMAP.md
  issues/
    ISSUE-NNN-<slug>.md             # global sequence across the whole repo, not per-module
  reports/
    README.md
    STATE-OF-IPED-<date>.md         # dated backups of previous STATE-OF-IPED.md versions
<module>/README.md                  # every module/submodule, links to its docs/roadmaps file
```

`docs/roadmaps/README.md` lists every roadmap file by workstream — check it if you're unsure whether a module already has a roadmap, or what it's named.

## Working on an existing module's roadmap

1. Find its file: `docs/roadmaps/<module>-ROADMAP.md` (module name = the Maven artifact directory name, e.g. `iped-engine-core`, not the parent path). A few exceptions: `iped-engine` has two extra sub-roadmaps (`iped-engine-plugin-registry-ROADMAP.md`, `iped-engine-schemas-ROADMAP.md`); the project-wide web UI delivery plan lives at `web-ui-implementation-ROADMAP.md` (not tied to one module).
2. Read the whole file before editing — `Module purpose` and `Current state`/`Progress checks` sections are narrative and should stay untouched unless the facts they describe actually changed.
3. Each `## Phase N — Title — \`status\`` section lists its issues. Follow the general issue-tracker skill's rules for creating/updating issues and rolling up phase status from them.
4. The `> Status legend:` line at the top of every roadmap already points to `docs/issues/` — don't reintroduce `[ ]`/`[~]`/`[x]` checkboxes inline; those were migrated away specifically so status lives in one place (the issue file) instead of two.

## Adding a new module

1. Create `docs/roadmaps/<module>-ROADMAP.md`. If there's no prior planning doc to migrate, start it with a `Module purpose` line, a `Current state` section (can be empty/minimal for a brand-new module), and at least one `Phase 1` section with its first issue(s).
2. Create `<module>/README.md` with a short purpose paragraph and a `## Roadmap` section linking to the file from step 1 (relative path depth depends on nesting — top-level modules use `../docs/roadmaps/...`, modules under `iped-engine-parent/*` use `../../docs/roadmaps/...`).
3. Add the module to `docs/roadmaps/README.md`'s index under the right workstream grouping, and to the root `ROADMAP.md`'s "Per-Module Roadmaps" section.

## Issue numbering

IDs are global across the whole repo (`ISSUE-001`, `ISSUE-002`, ...), not per-module — check the highest existing ID across all of `docs/issues/` before allocating a new one, the same as the general skill describes. Gaps in the sequence are fine and already exist (left intentionally during the initial consolidation); don't try to fill them in.

## Sizing issues

Default to one issue per genuinely PR-sized work item. If you're migrating a long flat checklist (more than ~10-15 closely related items with no phase breakdown) into this format, group related items into a handful of issues with the checklist folded in as that issue's acceptance criteria, rather than one issue per line — `iped-engine-schemas-ROADMAP.md` is a worked example of this (93 raw checklist items became 10 issues).

## Regenerating STATE-OF-IPED.md

The report at the repo root is a mechanical rollup of every issue's `Status:` field, grouped by the module its `Roadmap:` link points to, plus a manual read of `docs/roadmaps/*.md` for workstream highlights and risks (carried from the master `ROADMAP.md`'s own risk section). To regenerate:

1. Copy the current root `STATE-OF-IPED.md` to `docs/reports/STATE-OF-IPED-<date-it-was-generated>.md` (read its own header for that date) — skip this step only if no root report exists yet.
2. Recompute the per-module and overall status counts from `docs/issues/*.md`.
3. Re-write the root `STATE-OF-IPED.md` using the same structure (headline numbers, per-module table, workstream highlights, top risks, notable blocked issues, "how this was built" footer).
4. Don't hand-wave the numbers — count them (grep/script the `Status:` fields) rather than estimating from memory of the roadmaps.

## Verifying consistency

Before finishing any change here, the same checks the general issue-tracker skill describes apply repo-wide:
- Every `ISSUE-NNN` referenced in a roadmap has a matching file in `docs/issues/`, and vice versa.
- No two issue files share the same ID.
- Every module directory has a `README.md` linking to a `docs/roadmaps/` file.
- A phase's rolled-up status matches what its linked issues actually say.
