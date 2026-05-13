---
name: spec-to-implementation-playbook
description: Transform approved functional and technical specifications into a concrete implementation playbook for a modern stack. Use when a spec-driven migration needs epics, module backlogs, PR-sized work items, sequencing, dependencies, test strategy, and delivery checkpoints aligned with architecture decisions.
---

# Spec To Implementation Playbook

Use this skill after spec extraction is complete and architecture/technology decisions are known.

## Workflow

1. Validate inputs.
- Confirm that specs include entities, rules, validations, use cases, and acceptance criteria.
- Confirm architecture constraints (target stack, integration model, non-functional goals).

2. Build implementation mapping.
- Map each legacy capability to target modules and layers (frontend, backend, infra, data).
- Record migration approach per capability: rewrite, adapt, defer, retire.

3. Create global epics.
- Create one epic per major capability/bounded context.
- Ensure epic statements are outcome-focused and measurable.

4. Create module backlogs.
- Split each epic into module-specific items sized for a single PR.
- For each item, include: scope, touched components, acceptance criteria, test requirements, dependencies, risk notes.

5. Sequence delivery waves.
- Order items by architectural dependencies and value delivery.
- Define minimum vertical slices and parity milestones.

6. Define quality gates.
- Include unit, integration, contract, E2E, and non-functional validation gates.
- Add explicit exit criteria for each wave.

7. Finalize implementation playbook.
- Publish roadmap, module backlogs, PR plans, and release checkpoints.
- Keep traceability from spec IDs to backlog items.

## Output Contract

Generate or update these artifacts:
- `00-architecture-alignment.md`
- `10-epic-roadmap.md`
- `20-module-backlogs/<module>.md`
- `30-pr-plan/<epic-or-module>.md`
- `40-delivery-waves.md`

Use `scripts/init_implementation_playbook.py` to scaffold files.

## Sizing Rules (PR-Oriented)

- Keep each backlog item small enough for one PR review cycle.
- If an item spans multiple architectural concerns, split by concern boundary.
- Include explicit "out-of-scope" line in every backlog item.

## Traceability Rules

- Every epic must reference at least one spec capability or use-case ID.
- Every module backlog item must reference epic ID and spec IDs.
- Every PR plan entry must reference module backlog IDs.

## References

- Read `references/implementation-artifacts.md` for artifact schemas.
- Read `references/sizing-and-sequencing.md` for split and dependency heuristics.

## Quick Start

```bash
python scripts/init_implementation_playbook.py --output docs/implementation-playbook --project "ace-monorepo"
```

Then map extracted specs into these artifacts.
