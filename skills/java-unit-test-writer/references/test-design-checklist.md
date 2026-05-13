# Unit Test Design Checklist

Use this checklist before finalizing generated tests.

## Scope

- Target class/method is explicitly identified.
- Existing related tests were reviewed before adding new ones.
- Test file is in the correct module/package.

## Behavior Coverage

- Happy path is covered.
- Input boundary conditions are covered.
- Null/empty/invalid input scenarios are covered.
- Expected exception scenarios use `assertThrows`.
- Side effects/state changes are asserted when applicable.

## Dependency Handling

- External dependencies are mocked (I/O, network, parser engines, static/global state).
- Mockito stubbing is minimal and behavior-oriented.
- Critical collaborator interactions are verified when needed.

## Assertions and Readability

- Tests follow Arrange-Act-Assert.
- Test names follow `methodName_whenCondition_thenExpectedResult`.
- Assertions are specific (avoid weak assertions).
- Non-obvious logic has short comments.

## Determinism and Maintenance

- No dependence on real clock/randomness without control.
- No hidden order dependencies between tests.
- Shared setup is in `@BeforeEach` only when beneficial.
- Duplicate test logic is minimized.

## Build Readiness

- Imports are complete and clean.
- Test class compiles in module context.
- No TODO placeholders left in committed tests.

