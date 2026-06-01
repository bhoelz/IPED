# Code Coverage Report

Generated: 2026-05-31

## Summary

After running `mvn test` with JaCoCo coverage on selected modules (iped-api, iped-engine-core, iped-utils, iped-webapi, iped-mcp):

### Coverage by Module

| Module | Tests Run | Instruction Coverage | Branch Coverage | Line Coverage | Method Coverage | Status |
|--------|-----------|---------------------|---------------|---------------|---------------|--------|
| **iped-api** | 39 | 58.8% | 45.5% | 59.7% | 70.2% | ✅ Pass |
| **iped-engine-core** | 27 | 41.0% | 41.7% | 46.0% | 59.9% | ✅ Pass |
| **iped-webapi** | 3 | N/A | N/A | N/A | N/A | ❌ 1 Failure |
| **iped-utils** | 0 | N/A | N/A | N/A | N/A | ⚠️ No tests |
| **iped-mcp** | 0 | N/A | N/A | N/A | N/A | ⚠️ No tests |

### Test Results Details

#### iped-api (39 tests)
- All tests passed
- Instruction coverage: 58.8%
- Branch coverage: 45.5%
- Line coverage: 59.7%
- Method coverage: 70.2%

#### iped-engine-core (27 tests)
- All tests passed
- Instruction coverage: 41.0%
- Branch coverage: 41.7%
- Line coverage: 46.0%
- Method coverage: 59.9%

#### iped-webapi (3 tests)
- Tests run: 3
- Failures: 1
- Errors: 0
- **Failure:** `ArchitectureImportsTest.webApiMustNotImportEngineModuleClassesDirectly`
  - Forbidden engine imports found: `iped.engine.core.CaseContext`, `iped.engine.core.ProcessingOrchestrator`, `iped.engine.core.ResourceManager`
  - (JaCoCo report not generated due to test failure)

#### iped-utils (0 tests)
- No tests executed in this module
- (JaCoCo report not generated)

#### iped-mcp (0 tests)
- No tests executed in this module
- (JaCoCo report not generated)

---

## Overall Statistics

| Metric | Value |
|--------|-------|
| **Total Modules Tested** | 5 |
| **Modules with Coverage Reports** | 2 |
| **Total Tests Run** | 69 |
| **Tests Passed** | 68 |
| **Tests Failed** | 1 |
| **Average Instruction Coverage** | ~50% |
| **Average Branch Coverage** | ~44% |
| **Average Line Coverage** | ~53% |
| **Average Method Coverage** | ~65% |

---

## Notes

1. **JaCoCo was added to the parent pom.xml** to enable code coverage reporting.
2. **iped-webapi failed** due to an architecture test that checks for forbidden imports from `iped.engine.core`. This is an intentional design constraint.
3. **iped-utils** and **iped-mcp** had no test execution data, suggesting either:
   - No test classes exist
   - Tests were skipped
   - Compilation issues
4. Coverage reports are located in:
   - `iped-api/target/site/jacoco/`
   - `iped-engine-core/target/site/jacoco/`

---

## Recommendation

To improve code coverage:
1. Add unit tests for iped-utils and iped-mcp modules
2. Fix the architecture import issue in iped-webapi or update the test expectations
3. Target 70%+ line coverage for critical modules (iped-api, iped-engine-core)
4. Investigate branch coverage gaps (currently 41-45%)

---

## How to Reproduce

```bash
# Run tests with coverage (after JaCoCo is configured in pom.xml)
mvn clean test

# View HTML reports
open iped-api/target/site/jacoco/index.html
open iped-engine-core/target/site/jacoco/index.html
```
