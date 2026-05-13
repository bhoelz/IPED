---
name: reverse-spec-extractor
description: Extract implementation-ready specifications from a legacy codebase through reverse engineering. Use when planning a rewrite or migration and when you need maximum coverage of entities, business rules, validations, test cases, acceptance criteria, and traceability from code evidence to spec artifacts.
---

# Reverse Spec Extractor

Use this skill to convert legacy code and runtime behavior into a spec pack that can drive a new implementation.

## Workflow

1. Define scope and boundaries.
- Identify modules, bounded contexts, and integration points.
- Record in `00-context.md` the target scope, excluded scope, and assumptions.

2. Build evidence inventory.
- Inspect source files, API contracts, DB mappings, configuration, logs, and tests.
- Capture evidence references with absolute or repo-relative paths and line anchors when possible.

3. Extract domain entities and relationships.
- Produce entities, value objects, enums, aggregates, states, and key transitions.
- Map each extracted item to concrete evidence.

4. Extract business rules.
- Capture invariants, workflow rules, authorization rules, temporal constraints, and cross-module constraints.
- Format each rule with: id, statement, trigger, preconditions, outcome, exceptions, evidence.

5. Extract validations.
- Capture input, domain, and persistence validations.
- Include source of validation (UI, API, service, repository, DB/index).
- Include error message/response shape when available.

6. Extract testable behavior.
- Derive use cases, alternate flows, failure scenarios, and edge cases.
- Build acceptance criteria in Gherkin style.
- Build a parity-oriented test catalog for the new stack.

7. Build traceability matrix.
- Ensure every entity, rule, validation, and acceptance criterion references legacy evidence.
- Flag unresolved items with `TBD-EVIDENCE` and keep them explicit.

8. Run quality gate.
- Validate completeness with `references/extraction-checklist.md`.
- Do not close extraction until all mandatory checklist sections are marked done or explicitly deferred.

## Output Contract

Generate or update these files inside a spec pack folder:
- `00-context.md`
- `10-entities.md`
- `20-business-rules.md`
- `30-validations.md`
- `40-use-cases.md`
- `50-test-catalog.md`
- `60-acceptance.feature`
- `70-traceability.csv`

Use `scripts/init_spec_pack.py` to scaffold this structure.

## Operating Rules

- Prefer extraction over interpretation. Mark uncertainty explicitly.
- Keep one source of truth per fact. Avoid duplicate contradictory statements.
- Use stable IDs:
  - Entity: `ENT-###`
  - Rule: `BR-###`
  - Validation: `VAL-###`
  - Use case: `UC-###`
  - Test: `TC-###`
  - Acceptance criterion: `AC-###`
- Write concise statements that can be turned into tests without rewording.
- Separate discovered behavior from desired future behavior.

## References

- Read `references/spec-pack-structure.md` for required file schemas.
- Read `references/extraction-checklist.md` before finalizing extraction.

## Quick Start

```bash
python scripts/init_spec_pack.py --output docs/specs/legacy-baseline --project "ace-monorepo"
```

Then fill the generated files using the workflow above.
