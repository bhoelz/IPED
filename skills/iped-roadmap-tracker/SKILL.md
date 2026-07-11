---
name: iped-roadmap-tracker
description: Maintain IPED's planning docs — docs/roadmaps/<module>-ROADMAP.md per module, docs/issues/ISSUE-NNN-*.md for PR-sized work items, and the root STATE-OF-IPED.md/.html rollup report (Markdown source + a self-contained HTML render of the same data). Use this whenever working on IPED's own roadmaps or issues, e.g. "update the iped-engine roadmap", "create an issue for this in IPED's tracker", "what's the status of iped-distributed", "regenerate the state of IPED report", "update the HTML report", "this module needs a roadmap/README", or when a module's ROADMAP.md/README.md/checkbox list needs to follow the conventions established in the 2026-06 planning consolidation. Builds on the general issue-tracker skill's conventions, specialized to this repo's exact paths and module list.
---

# IPED Roadmap Tracker

IPED's planning docs were consolidated (2026-06) into one consistent structure: every module has a roadmap under `docs/roadmaps/`, every roadmap phase links to PR-sized issues under `docs/issues/`, every module's `README.md` points at its roadmap, and a generated `STATE-OF-IPED.md` (plus a `STATE-OF-IPED.html` render of the same data) rolls everything up. This skill is the IPED-specific specialization of the general `issue-tracker` skill — read that one too if it's available; this one only covers what's particular to this repo.

## Where things live

```
ROADMAP.md                          # master roadmap, project root, cross-cutting workstreams
STATE-OF-IPED.md                    # generated rollup report (source of truth), project root
STATE-OF-IPED.html                  # self-contained HTML render of the same report, project root
docs/
  roadmaps/
    README.md                       # index of all roadmaps
    <module>-ROADMAP.md             # one per module/submodule, e.g. iped-ad1-ROADMAP.md
  issues/
    ISSUE-NNN-<slug>.md             # global sequence across the whole repo, not per-module
  reports/
    README.md
    STATE-OF-IPED-<date>.md         # dated backups of previous STATE-OF-IPED.md versions (Markdown only — the HTML render is never backed up, just regenerated)
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

## When to regenerate

Treat `STATE-OF-IPED.md`/`.html` as derived, cached output — they go stale the moment any of their inputs change:
- An issue's `Status:` field changes (including new issues being created, e.g. `done`→cancelled, or a fresh `proposed`/`planned` issue added to a roadmap).
- A roadmap's phase structure changes (new phase, phase reassigned a different rollup status).
- A module is added, removed, or gets its roadmap/README created for the first time.

If you've just finished a roadmap/issue edit and didn't also touch `STATE-OF-IPED.md`, regenerate it (and the HTML render) before calling the task done — don't leave the report stale on the assumption "someone will regenerate it later." Both files are checked into git, so this is a normal part of the same commit/PR, not a separate maintenance task.

## Regenerating STATE-OF-IPED.md (Markdown — source of truth)

The report at the repo root is a mechanical rollup of every issue's `Status:` field, grouped by the module its `Roadmap:` link points to, plus a manual read of `docs/roadmaps/*.md` for workstream highlights and risks (carried from the master `ROADMAP.md`'s own risk section). To regenerate:

1. Copy the current root `STATE-OF-IPED.md` to `docs/reports/STATE-OF-IPED-<date-it-was-generated>.md` (read its own header for that date) — skip this step only if no root report exists yet.
2. Recompute the per-module and overall status counts from `docs/issues/*.md`.
3. Re-write the root `STATE-OF-IPED.md` using the same structure (headline numbers, per-module table, workstream highlights, top risks, notable blocked issues, "how this was built" footer), bumping the date in the `# State of IPED — <date>` heading to today.
4. Don't hand-wave the numbers — count them (grep/script the `Status:` fields) rather than estimating from memory of the roadmaps.

## Regenerating STATE-OF-IPED.html (render — always rebuilt after the Markdown)

`STATE-OF-IPED.html` is a self-contained, dark-themed render of exactly the data in `STATE-OF-IPED.md` — same headline stats, same per-module table (with progress bars and links back to each `docs/roadmaps/*.md`), same workstream highlights/risks/blocked-issues sections, same "how this was built" footer. It has no build step (no bundler, no external assets) and is meant to be opened directly in a browser.

**Never edit the HTML by hand or let it drift from the Markdown** — always regenerate it immediately after step 3 above, from the *new* Markdown content, not the old HTML. To regenerate:

1. Re-read the freshly-written `STATE-OF-IPED.md` in full.
2. Update the HTML's headline stat strip (the six `.stat` boxes — done/planned/in_progress/blocked/proposed/cancelled counts and percentages) to match the new headline numbers.
3. Update the `rows` array in the `<script>` block — one entry per module/cross-cutting roadmap row, in the same order as the Markdown table: `[name, total, done, in_progress, planned, blocked, roadmap-link-path]`. The table itself is rendered from this array at load time, so don't hand-edit `<tr>` markup.
4. Update the prose sections (workstream highlights, top risks, notable blocked issues, the subtitle's date) to match the Markdown verbatim in meaning — these are static HTML, not script-driven, so re-copy them by hand.
5. Don't add a stale-data warning banner or comment — if you regenerated correctly, the HTML matches the Markdown, full stop. If you *can't* regenerate (e.g. only asked to render an old snapshot), say so to the user instead of writing it into the file.

## Verifying consistency

Before finishing any change here, the same checks the general issue-tracker skill describes apply repo-wide:
- Every `ISSUE-NNN` referenced in a roadmap has a matching file in `docs/issues/`, and vice versa.
- No two issue files share the same ID.
- Every module directory has a `README.md` linking to a `docs/roadmaps/` file.
- A phase's rolled-up status matches what its linked issues actually say.
