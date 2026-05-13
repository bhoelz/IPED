# Extraction Checklist

Use this checklist as a quality gate before marking reverse specification as complete.

## Coverage
- [ ] Scope and assumptions documented.
- [ ] All in-scope modules inventoried.
- [ ] All external integrations inventoried.

## Entities
- [ ] Core entities extracted with attributes and identifiers.
- [ ] Relationships and ownership boundaries extracted.
- [ ] Entity lifecycle states and transitions captured.

## Business Rules
- [ ] Functional rules extracted from code behavior.
- [ ] Authorization and visibility rules extracted.
- [ ] Temporal and consistency constraints extracted.
- [ ] Rule conflicts or ambiguities explicitly flagged.

## Validations
- [ ] Input validations mapped by endpoint/form.
- [ ] Domain validations mapped by service/application layer.
- [ ] Persistence/index constraints mapped.
- [ ] Error payloads and user-facing messages captured when available.

## Tests and Acceptance
- [ ] Main flows represented by acceptance scenarios.
- [ ] Failure flows represented by acceptance scenarios.
- [ ] Edge-case scenarios documented.
- [ ] Regression/parity test catalog defined.

## Traceability
- [ ] Every key artifact has legacy evidence references.
- [ ] Low-confidence items flagged.
- [ ] `TBD-EVIDENCE` items listed with follow-up actions.

