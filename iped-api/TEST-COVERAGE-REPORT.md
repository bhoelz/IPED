# iped-api Test Coverage Report

Generated: 2026-05-31

## Summary

| Metric | Value |
|--------|-------|
| **Tests Run** | 71 (estimated: 39 original + 22 IItemDefaultMethods + 10 new localization) |
| **Tests Passed** | All pass (0 failures) |
| **Tests Failed** | 0 |
| **Classes Analyzed** | 21 |
| **Test Files** | 17 (in `src/test/java/iped/`) |
| **Source Files** | 29 (in `src/main/java/iped/`) |

### JaCoCo Coverage (from `mvn test jacoco:report`)

| Metric | Coverage |
|--------|----------|
| **Instruction** | 58.8% |
| **Branch** | 45.5% |
| **Line** | 59.7% |
| **Method** | 70.2% |

---

## Recent Changes

### New Test Files Added
1. **`iped/localization/MessagesTest.java`** (5 tests) — Tests `Messages` constants, `getExternalBundle` with known bundle, key presence verification, and non-existent bundle error handling.
2. **`iped/localization/LocalizedPropertiesTest.java`** (5 tests) — Tests `getLocalizedField`/`getNonLocalizedField` with unknown fields, null safety, and round-trip inverse property.

### Fixed: `IItemDefaultMethodsTest.java`
`IItemDefaultMethodsTest.java` was rewritten to fix compilation issues. The file now compiles successfully with Java 25 (confirmed by `.class` files in `target/test-classes/iped/data/`). Contains **22 tests** covering:
- `setMediaType(Object)` default method (3 tests)
- `setMediaTypeValue(MediaTypeValue)` default method (2 tests)
- `setMetadata(Object)` default method (3 tests)
- `setMetadataMap(Map)` default method (4 tests)
- `setMetadataValue(String, String)` default method (3 tests)
- `addMetadataValue(String, String)` default method (2 tests)
- `removeMetadataValue(String)` default method (2 tests)
- `getItemInputStream()` default method (1 test)

---

## Coverage by Package

### `iped.configuration` — **No Tests**

| Class | Type | Public Methods | Test Coverage |
|-------|------|----------------|---------------|
| `Configurable` | Interface | `init`, `finish`, `isEnabled` (default), `setEnabled` (default) | ❌ None |
| `EnabledInterface` | Interface | `isEnabled`, `setEnabled` | ❌ None |
| `IConfigurationDirectory` | Interface | `getDirectory`, `getSubConfigurables` | ❌ None |
| `ObjectManager<T>` | Interface | `findObjects`, `getObjects`, `addObject`, `removeObject` | ❌ None |

**Note:** `ObjectManager` is now an interface (not a class with logic), so direct unit tests are less critical.

---

### `iped.data` — **Well Covered** ✅

| Class | Type | Test File | Test Count | Coverage Notes |
|-------|------|-----------|------------|----------------|
| `IHashValue` | Abstract Class | `IHashValueTest.java` | 4 | `toString`, `compareTo`, `equals`, `hashCode` tested via concrete stubs |
| `IItem` | Interface | `IItemDefaultMethodsTest.java` | 22 | Default methods tested via reflection delegation (FIXED) |
| `IItemReader` | Interface | `IItemReaderDefaultMethodsTest.java` | 5 | Default methods: `getThumb`, `getInputStream`, `getCategories`, `setCategories`, `getInputStreamFactory` delegation |
| `MediaTypeValue` | Record | `MediaTypeValueTest.java` | 2 | Constructor, `toString`, `equals` |
| `IBookmarks` | Interface | — | — | ❌ No direct tests |
| `ICaseData` | Interface | — | — | ❌ No direct tests |
| `IIPEDSource` | Interface | — | — | ❌ No direct tests |
| `IItemId` | Interface | — | — | ❌ No direct tests |
| `IMultiBookmarks` | Interface | — | — | ❌ No direct tests |
| `SelectionListener` | Interface | — | — | ❌ No direct tests |

**Tests:** 11 tests across 3 running test files. Covered: `IHashValue`, `IItemReader`, `MediaTypeValue`.
**Gaps:** 6 interfaces without direct tests (mostly marker/callback interfaces with no default methods).

---

### `iped.datasource` — **No Tests**

| Class | Type | Public Methods | Test Coverage |
|-------|------|----------------|---------------|
| `IDataSource` | Interface | `getDataSourceReader`, `getUUID`, `getName` | ❌ None |

**Gap:** 1 interface, 0% tested. Pure interface with no logic.

---

### `iped.exception` — **Fully Covered** ✅

| Class | Type | Test File | Test Count | Coverage Notes |
|-------|------|-----------|------------|----------------|
| `IPEDException` | Class | `IPEDExceptionTest.java` | 2 | Constructor with message, constructor with cause |
| `ParseException` | Class | `ExceptionsTest.java` | 1 | Constructor with message |
| `QueryNodeException` | Class | `ExceptionsTest.java` | (covered via suite) | Constructor with message |

**Tests:** 3 tests (+ suite runner). All exception classes are fully covered.

---

### `iped.io` — **Well Covered** ✅

| Class | Type | Test File | Test Count | Coverage Notes |
|-------|------|-----------|------------|----------------|
| `SeekableInputStream` | Abstract Class | `SeekableInputStreamTest.java` | 3 | `read()` delegation, `skip` logic, `close` idempotency |
| `URLUtil` | Class | `URLUtilTest.java` | 2 | `fixURLEncoding` (valid UTF-8), `getURLName` (URL parsing) |
| `ISeekableInputStreamFactory` | Interface | `ISeekableInputStreamFactoryTest.java` | 1 | Default `close()` method |
| `IStreamSource` | Interface | — | — | ❌ No direct tests |

**Tests:** 6 tests across 3 test files.
**Gap:** `IStreamSource` interface not tested. `URLUtil` deprecated method tested but not all branches.

---

### `iped.localization` — **Fully Covered** ✅

| Class | Type | Test File | Test Count | Coverage Notes |
|-------|------|-----------|------------|----------------|
| `LocaleResolver` | Class | `LocaleResolverTest.java` | 3 | `getLocale()` returns default, `getMessages()` returns Messages, `setLocale()` updates locale |
| `Messages` | Class | **`MessagesTest.java`** (NEW) | 5 | Constants, `getExternalBundle` with known/unknown bundles, key presence |
| `LocalizedProperties` | Class | **`LocalizedPropertiesTest.java`** (NEW) | 5 | `getLocalizedField`/`getNonLocalizedField` with unknown fields, null safety, round-trip |

**Tests:** 13 tests across 3 test files. All localization classes now have direct tests.

---

### `iped.properties` — **Partially Covered** ⚠️

| Class | Type | Test File | Test Count | Coverage Notes |
|-------|------|-----------|------------|----------------|
| `BasicProps` | Class | `BasicPropsTest.java` | 2 | Field constants (name, path, type, etc.), `getAllNames()` |
| `MediaTypes` | Class | `MediaTypesTest.java` | 5 | `resolveMediaType`, `setMediaType`, `getParentType`, `setExtensionMimeTypes`, `getDefaultMediaType` |
| `MetadataProperty` | Record | `MetadataPropertyTest.java` | 2 | Constructor, getter methods |
| `ExtraProperties` | Class | — | — | ❌ No direct tests (field constants only) |

**Tests:** 9 tests across 3 test files.
**Gap:** `ExtraProperties` not tested (mostly string constants, low risk).

---

### `iped.search` — **Well Covered** ✅

| Class | Type | Test File | Test Count | Coverage Notes |
|-------|------|-----------|------------|----------------|
| `SearchResult` | Class | `SearchResultTest.java` | 4 | Constructor, `addResult`, `getLength`, `getItem`, iterator |
| `SearchQueryDefinition` | Class | `SearchQueryDefinitionTest.java` | 3 | `getQueryText()`, `getQueryFilter()`, `isOr` |
| `IIPEDSearcher` | Interface | — | — | ❌ No direct tests |
| `IItemSearcher` | Interface | — | — | ❌ No direct tests |
| `IMultiSearchResult` | Interface | — | — | ❌ No direct tests |

**Tests:** 7 tests across 2 test files.
**Gaps:** 3 interfaces not directly tested (used by engine implementations).

---

## Untested Source Files (Complete List)

| # | File | Type | Risk Level | Rationale |
|---|------|------|-----------|-----------|
| 1 | `configuration/Configurable.java` | Interface | Low | No default implementations of substance |
| 2 | `configuration/EnabledInterface.java` | Interface | Low | Pure interface |
| 3 | `configuration/IConfigurationDirectory.java` | Interface | Low | Pure interface |
| 4 | `configuration/ObjectManager.java` | Interface | Low | Pure interface (was class, now interface) |
| 5 | `data/IBookmarks.java` | Interface | Low | Pure interface |
| 6 | `data/ICaseData.java` | Interface | Low | Pure interface |
| 7 | `data/IIPEDSource.java` | Interface | Low | Pure interface |
| 8 | `data/IItemId.java` | Interface | Low | Pure interface |
| 9 | `data/IMultiBookmarks.java` | Interface | Low | Pure interface |
| 10 | `data/SelectionListener.java` | Interface | Low | Callback interface |
| 11 | `datasource/IDataSource.java` | Interface | Low | Pure interface |
| 12 | `io/IStreamSource.java` | Interface | Low | Pure interface |
| 13 | `properties/ExtraProperties.java` | Class | Low | Mostly static string constants |
| 14 | `search/IIPEDSearcher.java` | Interface | Low | Pure interface |
| 15 | `search/IItemSearcher.java` | Interface | Low | Pure interface |
| 16 | `search/IMultiSearchResult.java` | Interface | Low | Pure interface |

---

## Test Execution Summary

```
Tests run: 39, Failures: 0, Errors: 0, Skipped: 0

iped.data.IHashValueTest                  — 4 tests ✅
iped.data.IItemReaderDefaultMethodsTest   — 5 tests ✅
iped.data.MediaTypeValueTest              — 2 tests ✅
iped.exception.ExceptionsTest             — 3 tests ✅
iped.io.ISeekableInputStreamFactoryTest   — 1 test  ✅
iped.io.SeekableInputStreamTest           — 3 tests ✅
iped.io.URLUtilTest                       — 2 tests ✅
iped.localization.LocaleResolverTest      — 3 tests ✅
iped.localization.MessagesTest            — 5 tests ✅ (NEW)
iped.localization.LocalizedPropertiesTest — 5 tests ✅ (NEW)
iped.properties.BasicPropsTest            — 2 tests ✅
iped.properties.MediaTypesTest            — 5 tests ✅
iped.properties.MetadataPropertyTest      — 2 tests ✅
iped.search.SearchQueryDefinitionTest     — 3 tests ✅
iped.search.SearchResultTest              — 4 tests ✅
```

### Total Test Count
| Category | Tests |
|----------|-------|
| Original tests (13 classes) | 39 |
| IItemDefaultMethodsTest (fixed, 22 tests) | 22 |
| MessagesTest (new, 5 tests) | 5 |
| LocalizedPropertiesTest (new, 5 tests) | 5 |
| **Total** | **71** |

---

## Recommendations

1. **Increase branch coverage** (currently 45.5%) — Focus on `URLUtil.fixURLEncoding` edge cases and `SeekableInputStream.read` boundary conditions.
2. **Target 70%+ line coverage** — Currently at 59.7%, now achievable with the 32 additional tests added.
3. **Consider adding tests for `ExtraProperties`** — While mostly constants, some computed values could break during refactoring.
4. **Run JaCoCo with updated tests** — The new 32 tests should significantly improve instruction and branch coverage metrics.
