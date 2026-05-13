# Implementation Playbook Artifacts

## 00-architecture-alignment.md
- Link to target architecture decisions.
- Map bounded contexts to target modules/services.
- Record non-functional requirements and constraints.

## 10-epic-roadmap.md
- One row per epic.
- Fields: epic ID, objective, linked spec IDs, status, dependencies, target wave.

## 20-module-backlogs/<module>.md
- PR-sized items only.
- Fields: item ID, epic ID, title, scope, out-of-scope, acceptance refs, tests, dependencies.

## 30-pr-plan/<epic-or-module>.md
- Tactical sequence of pull requests.
- Fields: PR order, files/components, migration risk, rollback notes.

## 40-delivery-waves.md
- Delivery increments with entry/exit criteria.
- Include quality gates and parity checkpoints per wave.

