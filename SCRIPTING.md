# IPED Scripting Guide

This document describes scripting/extensibility points in IPED for Python and JavaScript, what each one can do, which functions are expected, and where each feature is implemented.

## Overview

IPED supports scripting in four places:

1. Processing pipeline tasks (`scripts/tasks`): Python (`.py`) and JavaScript (`.js`).
2. Tika parsers (`scripts/parsers`): Python parsers loaded by `iped.parsers.python.PythonParser`.
3. Regex validators (`scripts/regex_validators`): JavaScript validators for `RegexTask`.
4. File carving behavior (`carvers/*.xml` + JavaScript files): JavaScript-based carvers.

Main implementation modules:

- Task script loading and lifecycle:
  - `iped-engine/src/main/java/iped/engine/config/TaskInstallerConfig.java`
  - `iped-engine/src/main/java/iped/engine/task/ScriptTask.java`
  - `iped-engine/src/main/java/iped/engine/task/PythonTask.java`
- Python runtime integration (JEP):
  - `iped-parsers/iped-parsers-impl/src/main/java/iped/parsers/python/PythonParser.java`
- Regex validator scripting:
  - `iped-engine/src/main/java/iped/engine/task/regex/validator/ScriptValidatorService.java`
- Carver scripting:
  - `iped-engine/src/main/java/iped/engine/task/carver/XMLCarverConfiguration.java`
  - `iped-carvers/iped-carvers-impl/src/main/java/iped/carvers/standard/JSCarver.java`

## 1) Processing Task Scripts (`scripts/tasks`)

### Where scripts are discovered

- Task list is declared in `TaskInstaller.toml` (example: `iped-app/resources/config/conf/TaskInstaller.toml`).
- Script entries are list items ending in `.js` or `.py` (e.g. `"RefineCategoryTask.js"`).
- Script lookup order is:
  1. `<case config>/scripts/tasks`
  2. `<app root>/scripts/tasks`
- Defined by `TaskInstallerConfig.SCRIPT_BASE = "scripts/tasks"`.

### Supported languages

- `.py` -> `PythonTask`
- any other extension (typically `.js`) -> `ScriptTask`

### Lifecycle and required functions

#### JavaScript task (`ScriptTask`)

Expected global functions in the script:

- `getName() -> String` (required)
- `getConfigurables() -> List<Configurable<?>> | null` (optional in practice, but called by Java side)
- `init(configurationManager)` (required by engine call path)
- `process(item)` (required)
- `finish()` (required by engine call path)

See example: `iped-app/resources/scripts/tasks/ExampleScriptTask.js`.

Injected objects/variables:

- Before `init()`:
  - `caseData`
  - `moduleDir`
  - `worker`
  - `stats`
- Before `finish()`:
  - `ipedCase` (`IPEDSource`)
  - `searcher` (`IPEDSearcher`)

Notes:

- Script is loaded with `ScriptEngineManager.getEngineByExtension(ext)` and executed once per task instance/thread.
- `finish()` clears engine bindings; shared `ipedCase` is closed when last instance finishes.

#### Python task (`PythonTask`)

Expected Python structure:

- File name without extension must match class name.
  - Example: `PythonScriptTask.py` must contain class `PythonScriptTask`.
- The class is instantiated per worker/thread by `PythonTaskInstancesHolder`.

Expected methods:

- `isEnabled(self) -> bool` (optional; defaults to enabled when method is absent)
- `getConfigurables(self) -> list` (optional in practice)
- `init(self, configurationManager)` (called during load/init)
- `process(self, item)` (required)
- `finish(self)` (called if task enabled)
- `processQueueEnd(self) -> bool` (optional)
- `sendToNextTask(self, item)` (optional override hook)

See examples:

- `iped-app/resources/scripts/tasks/PythonScriptTask.py`
- `iped-app/resources/scripts/tasks/SearchHardwareWallets.py`

Runtime helper:

- `iped-app/resources/scripts/tasks/PythonTaskInstancesHolder.py`

Injected globals (module/class context):

- `caseData`
- `moduleDir`
- `worker`
- `stats`
- `logger`
- `javaConverter` (contains `toKnnVector(double[])`)
- `ImageUtil`
- `numThreads`

Injected before `finish()`:

- `ipedCase`
- `searcher`

Error handling / behavior:

- If JEP is unavailable, Python tasks are disabled (or throw if `throwExceptionInsteadOfLogging` is enabled by caller).
- Exceptions in `process()` are logged; some thread-access errors are rethrown.

### Configuration examples

- Default profile task pipeline:
  - `iped-app/resources/config/conf/TaskInstaller.toml`
- Triage profile pipeline:
  - `iped-app/resources/config/profiles/triage/conf/TaskInstaller.toml`

These files show built-in Python and JavaScript task scripts actively used by IPED.

## 2) Python Parser Scripts (`scripts/parsers`)

Python parser scripts are loaded by Apache Tika through `iped.parsers.python.PythonParser`.

### Enablement and discovery

- `ParserConfig.xml` includes:
  - `<parser class="iped.parsers.python.PythonParser"></parser>`
- During parsing setup, IPED sets:
  - `PythonParser.PYTHON_PARSERS_FOLDER = <appRoot>/scripts/parsers`
  - via `iped-engine/src/main/java/iped/engine/task/ParsingTask.java`

### Required parser class/functions

For `MyParser.py`, class must be `MyParser`.

Functions:

- `getSupportedTypes(context) -> list[str]` (required)
- `parse(stream, handler, metadata, context)` (required)
- `getSupportedTypesQueueOrder() -> dict[str, int]` (optional)

Example script:

- `iped-app/resources/scripts/parsers/PythonParserExample.py`

Queue order support:

- Optional `getSupportedTypesQueueOrder()` lets parser media types run in later queues.
- Integrated with `QueuesProcessingOrder` via `PythonParser.getMediaTypesToQueueOrder()`.

## 3) JavaScript Regex Validators (`scripts/regex_validators`)

Regex validators are loaded by `ScriptValidatorService`, which is registered as a `RegexValidatorService` provider.

### Discovery

- Validator folder relative to config dir:
  - `<confDir>/regex_validators`
- Usually provided in app resources under:
  - `iped-app/resources/scripts/regex_validators`

### Required functions in each validator script

- `getRegexNames() -> array`
- `validate(hit) -> boolean`
- `format(hit) -> string`

Example:

- `iped-app/resources/scripts/regex_validators/ExampleValidator.js`

Behavior:

- One script can serve multiple regex names.
- Missing validator for a regex name triggers an error at validation time.

## 4) JavaScript Carver Scripts

Carver definitions are loaded from XML by `XMLCarverConfiguration`.

If `<carverScriptFile>` is defined in a carver type, IPED sets:

- `carverClass = JSCarver`
- `carverScript = "carvers/<file>"` (relative script path handling in config loader)

At runtime, `CarverTask` instantiates the script carver through:

- `carverConfig.createCarverFromJSName(file)` -> `new JSCarver(file)`

Supported optional JS functions in a carver script (`JSCarver` calls these if present):

- `getLengthFromHeader(parentEvidence, header)`
- `validateCarvedObject(parentEvidence, header, length)`
- `carveFromHeader(parentEvidence, header)`
- `carveFromFooter(parentEvidence, footer)`
- `getCarverTypes()`
- `notifyHit(parentEvidence, hit)`
- `notifyEnd(parentEvidence)`

If not implemented, Java defaults from `DefaultCarver` are used for several operations.

## Runtime and Engine Details

## JavaScript engine

- JavaScript execution uses JSR-223 (`javax.script`) engine resolution by file extension.
- `iped-engine` includes `org.openjdk.nashorn:nashorn-core` dependency.
- Main entry points:
  - `ScriptTask`
  - `ScriptValidatorService`
  - `JSCarver`
  - `VideoThumbsMaker` (for JS equation evaluation in config)

## Python engine (JEP)

- Python scripting relies on `jep` (`SharedInterpreter`).
- On Windows, `PythonParser` configures a bundled Python home under `<ipedRoot>/python` and sets JEP native library path.
- `iped-app/pom.xml` includes packaging step/dependency for `python-jep-dlib`.

## What Is Scriptable Today (Practical)

You can currently script:

1. Item-level processing logic in the pipeline (tagging, filtering, enrichment, ML integrations).
2. Custom parsing of file formats into text/metadata through Python Tika parsers.
3. Regex post-validation/formatting to reduce false positives.
4. File carving behavior for signatures/headers/footers using JS carvers.

## Constraints and Caveats

1. Class/file naming convention is strict for Python task/parser scripts.
2. Most task hooks are called from Java and should be treated as required for robust scripts.
3. Script execution is multithreaded; thread safety is mandatory.
4. Python support depends on JEP availability/native libraries.
5. Engine objects exposed to scripts are Java objects; script code must interoperate with JVM types.

## Useful Built-in Script References

- Tasks:
  - `iped-app/resources/scripts/tasks/ExampleScriptTask.js`
  - `iped-app/resources/scripts/tasks/PythonScriptTask.py`
  - `iped-app/resources/scripts/tasks/RefineCategoryTask.js`
  - `iped-app/resources/scripts/tasks/IgnoreFilesByPathTask.js`
- Parsers:
  - `iped-app/resources/scripts/parsers/PythonParserExample.py`
- Regex validators:
  - `iped-app/resources/scripts/regex_validators/ExampleValidator.js`
  - `iped-app/resources/scripts/regex_validators/ExampleCryptoSeedPhraseValidator.js`
