# Spec Pack Structure

Use the following files as mandatory artifacts for reverse-engineered specifications.

## 00-context.md
- Scope, excluded scope, assumptions, glossary.
- Legacy modules and integrations under analysis.
- Evidence sources and confidence notes.

## 10-entities.md
- Entities, value objects, enums, relationships.
- Lifecycle states and transition rules.
- IDs: `ENT-###`.

## 20-business-rules.md
- Rule catalog with IDs `BR-###`.
- Fields: statement, trigger, preconditions, outcome, exceptions, evidence.

## 30-validations.md
- Validation catalog with IDs `VAL-###`.
- Fields: layer, condition, error output, severity, evidence.

## 40-use-cases.md
- Use cases with IDs `UC-###`.
- Main flow, alternate flows, failure flows, edge conditions.

## 50-test-catalog.md
- Tests with IDs `TC-###`.
- Mapping from tests to rules/validations/use-cases.
- Proposed automation level: unit, integration, contract, e2e.

## 60-acceptance.feature
- Gherkin scenarios with IDs `AC-###`.
- Reference rule/validation IDs in scenario comments.

## 70-traceability.csv
- Matrix columns:
  - ArtifactType
  - ArtifactID
  - Description
  - LegacyEvidencePath
  - LegacyEvidenceLine
  - Confidence
  - Notes

