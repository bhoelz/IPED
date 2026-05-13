---
name: java-unit-test-writer
description: Generate high-quality unit tests for this repository using Java 25, JUnit 6, and Mockito. Use when adding new tests, extending existing test suites, or increasing coverage for business logic with clear AAA structure and edge-case focus.
---

# Java Unit Test Writer

Use this skill to generate comprehensive, idiomatic unit tests for Java code in this repo.

## Runtime

- Java 25+
- JUnit 6
- Mockito

## Capabilities

1. Analyze Java class or method behavior from source code.
2. Infer happy paths, boundary conditions, and failure scenarios.
3. Generate test classes with:
- `@Test`
- `@BeforeEach`
- `@AfterEach`
- `@ParameterizedTest` where useful
4. Generate exception-path tests with `assertThrows`.
5. Mock dependencies with Mockito and verify interactions when behavior depends on collaborators.
6. Apply descriptive test naming:
- `methodName_whenCondition_thenExpectedResult`
7. Prefer JUnit Assertions and use AssertJ only when already present in the module.

## Inputs

- Required: Java source code (class or method scope).
- Optional: existing `*Test.java` file to extend.
- Optional: interface contracts, dependency graph, or behavioral notes.

## Outputs

1. A complete `*Test.java` file (or extension patch) with all required imports.
2. Focused inline comments only for non-obvious test logic.
3. A short coverage summary including:
- covered paths
- edge cases
- exception scenarios
- explicitly untestable parts (if any)

## Workflow

1. Identify target and test scope.
- Map target production file to corresponding test module/package.
- Detect existing test class before creating a new one.

2. Inspect behavior and seams.
- Read method branches, null handling, guards, and error propagation.
- Identify external dependencies to mock (I/O, network, parser engines, static state).

3. Design test matrix.
- Happy path.
- Boundary and corner cases.
- Invalid inputs and exception contracts.
- Interaction checks for mocked dependencies.

4. Implement tests using AAA.
- Arrange: setup inputs, fixtures, mocks.
- Act: execute only the behavior under test.
- Assert: verify outputs, state changes, exceptions, and interactions.

5. Keep tests deterministic.
- Avoid clocks/randomness unless controlled.
- Isolate file system and external services via mocks/stubs.

6. Validate quality gate before finalizing.
- Tests compile in module context.
- No dead imports or unused setup.
- Names and assertions are precise and readable.

## Constraints

- Do not claim coverage for paths not exercised by generated tests.
- Do not generate integration/end-to-end tests unless explicitly requested.
- Complex infra scenarios (database, filesystem-heavy flows, remote services) must be mocked in unit tests.
- Requires well-formed Java source input.

## Repo Conventions

1. Keep tests close to the module under test.
2. Preserve existing style in each module.
3. Prefer minimal mocking when pure logic can be asserted directly.
4. Add regression tests for discovered bugs before refactors.
