# Sizing and Sequencing Heuristics

## PR-Sized Item Rules
- Keep each item mergeable in one PR without hidden follow-up mandatory for correctness.
- Limit item scope to one primary concern (API, domain, UI flow, infra change, or test harness).
- Include explicit out-of-scope to prevent silent scope expansion.

## Dependency Rules
- Sequence by architectural dependency graph:
  1. Foundations (contracts, shared primitives)
  2. Domain core
  3. Integration adapters
  4. User workflows
  5. Hardening and optimization

## Traceability Rules
- Link every epic to one or more spec IDs (ENT/BR/VAL/UC/AC).
- Link every module item to one epic ID and related spec IDs.
- Link each PR plan line to module item IDs.

## Quality Gates
- Require tests and acceptance mapping before moving item to done.
- Require integration and contract checks before closing epic.
- Require parity checkpoint sign-off before release wave closure.

