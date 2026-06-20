# iped-parsers — Evolution Roadmap

> Module purpose: parent of all artifact parsers (~31 submodules): Tika extensions and
> forensic-specific parsers (registry, lnk, usnjrnl, browsers, P2P apps, mail, sqlite
> family, mobile chat apps, etc.), plus `iped-parsers-common/main/impl` aggregation.
> Status legend: `[ ]` planned · `[~]` in progress · `[x]` done

## Current state (2026-06)
- Per-parser modularization already done (one Maven module per parser family); built as
  plugins into the shared `plugins` folder (`plugin.dir` in the root pom).
- `iped-parsers-impl` still aggregates a long tail of parsers not yet split out.
- Tika 3.3 baseline.

## Phase 1 — Finish the per-parser split
- [~] Inventory done: 240 classes remain in `iped-parsers-impl`. Split candidates by
      priority (highest churn / most self-contained):
      1. `iped-parser-whatsapp` — 60+ classes, bencode + sqlite deps
      2. `iped-parser-telegram` — 10+ classes, telegram-decoder-api dep
      3. `iped-parser-threema` — 10+ classes
      4. `iped-parser-ufed` — 30+ classes for UFED XML extraction
      5. `iped-parser-compression` — SevenZipParser, RARParser, LZFSEParser, PackageParser
- [x] Prerequisites moved to `iped-parsers-common`:
      - `IParty` + `IReferencedContact` interfaces (`iped.parsers.chat`) — `Party`
        implements `IParty`, `ReferencedAccountable` implements `IReferencedContact`
      - `PartyStringBuilder` migrated to use `IParty` so it no longer touches impl
      - `ParserConstants.INDEXER_CONTENT_TYPE` constant extracted from `StandardParser`
      - `PhoneParsingConfig` moved from impl to common (only depends on `iped-api`)
- [x] `iped-parser-whatsapp` module created and compiles clean:
      28 parser classes + `WhatsAppPartyStringBuilder` + `com.whatsapp.MediaData` +
      CSS/JS/image resources moved; `META-INF/services` wired; `iped-parsers-impl`
      depends on it; `libfqlite`, `metadata-extractor`, BouncyCastle, Xerces deps
      declared; report-path helpers inlined into `iped.parsers.whatsapp.Util`.
- [x] `iped-parser-threema` module created and compiles clean:
      9 parser classes + CSS/JS/img resources moved; `META-INF/services` wired;
      report-path helpers (`getExportPath`, `getReportHref`, `getSourceFileIfExists`,
      `getItems`) added to local `Util`; `iped-parser-db-base` (ItemInfo),
      `iped-parser-plist-detector`, dd-plist, guava, jackson deps declared.
- [x] `iped-parser-telegram` module created and compiles clean:
      13 parser classes + resource (css/tooltip.css) + test fixtures moved;
      `META-INF/services` wired; `iped-parser-db-base` (ItemInfo), `iped-parser-vcard`
      (HTML_STYLE), `telegram-decoder-api` deps declared; `javax.xml.bind.DatatypeConverter`
      replaced with `Base64.getDecoder()`; `iped.parsers.whatsapp.Util.*` references
      inlined into `iped.parsers.telegram.Util`; report-path helpers replicated locally.
- [x] `iped-parser-ufed` module created and compiles clean:
      43 parser/handler/model/reference/util classes moved; `META-INF/services` wired for
      4 parsers; `HtmlParser` → `JSoupParser` (Tika 3.x); `StandardParser.INDEXER_CONTENT_TYPE`
      → `ParserConstants.INDEXER_CONTENT_TYPE`; `iped.parsers.util.Util.*` calls in
      `ReportGenerator` resolved via `iped.parsers.whatsapp.Util` (intentional dep —
      UFED chat report reuses WhatsApp visual template + resources).
      Moved to `iped-parsers-common`: `EmailPartyStringBuilder`, `GenericPartyStringBuilder`,
      `TelegramPartyStringBuilder`, `InstagramPartyStringBuilder`, `WhatsAppPartyStringBuilder`,
      `PartyStringBuilderFactory`, `OmitEmptyArraysTypeAdapterFactory`, `ConversationConstants`,
      `HashUtils`; added `IItemReader getItem()` to `IReferencedContact`; gson + commons-codec
      added to common pom. Also fixed pre-existing impl errors: `XMLParser` (`HtmlParser` →
      `JSoupParser`), `OFCParser` (`javax.xml.bind` via `jaxb-api:2.3.1`).
- [x] `iped-parser-compression` module created and compiles clean:
      `SevenZipParser`, `RARParser`, `LZFSEParser`, `PackageParser` + `RawISOConverter`
      moved; `META-INF/services` wired for 4 parsers; `iped.parsers.util.Util.getParentPath`
      replaced by local `CompressionUtil.getParentPath` (no impl circular dep);
      sevenzipjbinding, junrar, commons-compress, RagingMoose deps declared.
- [x] Per-module dependency hygiene pass:
      - Legacy `commons-lang` 2.6 removed from `iped-parser-registry`, `iped-parser-skype`,
        `iped-parser-whatsapp`, `iped-parsers-impl` (zero remaining usages in impl; the
        three parser modules had their `ArrayUtils`/`StringUtils` imports migrated to
        `commons-lang3`, already declared in each pom).
      - `sqlite-jdbc` moved to `runtime` scope in `iped-parser-eventtranscript`,
        `iped-parser-gdrive`, `iped-parser-winx` (no compile-time `org.sqlite.*` import —
        they only need the driver registered at runtime via JDBC `DriverManager`).
        Left at compile scope in `iped-parser-sqlite-core`/`iped-parser-sqlite-detector`
        (reference `org.sqlite.SQLiteConfig` directly) and `iped-parsers-impl`
        (`OCRParser` uses `org.sqlite.SQLiteConfig`).
      - `iped-parser-skype` no longer depends on `iped-parsers-impl`: its `ReportGenerator`
        only used `iped.parsers.util.Util.getItems`/`getExportPath`, now replicated in a
        local `iped.parsers.skype.Util` (same pattern as the threema/ufed/telegram splits).
        This also removes a backwards dependency edge (a split-out module depending on
        the impl aggregate that depends on it via the parser SPI).
- [x] ArchUnit guard added: `ParsersBoundaryTest` in `iped-parsers-impl` enforces that
      no parser class imports `iped.engine.*` or `iped.app.*`.
- [x] Parser plugin contract already exists in practice: every split-out module
      registers via the standard Tika SPI (`META-INF/services/org.apache.tika.parser.Parser`
      + `getSupportedTypes(ParseContext)` declaring MIME types); Tika's own composite/
      priority resolution governs ordering. No additional manifest format was introduced —
      third-party parser plugins ship the same way the in-tree split modules do (drop a jar
      with the services file on the plugins classpath). Carving got the equivalent
      contract this round: see `iped-carvers` Phase 3 `CarverPlugin` SPI.

## Phase 2 — Quality and coverage
- [~] Fixture-based regression corpus per parser module (small real-world samples, run in
      CI) — protects against Tika upgrades and refactors. Most modules already carry real
      sample-file fixtures (mail, browsers, registry, gdrive, security, shareaza, etc. —
      14+ modules with `src/test/resources/test-files`). Gap closed this round: the five
      recently split modules (`whatsapp`, `threema`, `ufed`, `compression`) had **zero**
      test coverage after the split; added baseline `getSupportedTypes()` +
      `META-INF/services` SPI-discovery smoke tests for each (`WhatsAppParserTest`,
      `ThreemaParserTest`, `UfedParsersTest`, `CompressionParsersTest`) — same convention
      already used for modules without real fixtures (e.g. `APKParserTest`).
      These tests immediately caught a real bug: **`UfedMessageParserTest` crashed with
      `NoClassDefFoundError`** because `MediaTypes.MEDIA_TYPE_REGISTRY`'s eager
      `TikaConfig.getDefaultConfig()` call was declared *before* the `MediaType` constants
      in the same class — `getDefaultConfig()` builds Tika's `DefaultParser`, which
      `ServiceLoader`-discovers and reflectively instantiates every registered parser
      including `UfedMessageParser`, which reads `MediaTypes.UFED_MESSAGE_MIME` in its own
      static initializer; that reentrant class-init saw the field still `null`. Fixed in
      `iped-api`'s `MediaTypes.java` by moving `MEDIA_TYPE_REGISTRY` to be declared after
      all the `MediaType` constants. Remaining gap: `whatsapp`/`threema`/`ufed` still lack
      *real* binary fixtures (sqlite DBs, UFED XML) — capturing authentic sample data is a
      data-acquisition task, deferred.
- [ ] Tika upgrade policy: track releases, run the full fixture corpus as the gate.
- [x] Standardize metadata property naming across parsers (audit drift against
      `iped.properties` definitions in `iped-api`). Audit found no typo/casing drift
      against the canonical `ExtraProperties`/`BasicProps` constants themselves — the
      real gap was a *different* hazard: `WhatsAppParser`, `ThreemaParser`,
      `SkypeParser`, and `TelegramParser` independently hand-typed the identical
      literal property names (`"chatId"`, `"mediaName"`, `"mediaMime"`, `"mediaSize"`,
      `"duration"`, `"messageStatus"`) for the same semantic chat-message concepts,
      with zero compiler safety net against one of them silently drifting (a future
      typo/case change would fragment the same logical property into two index
      fields). Added `CHAT_ID`/`MEDIA_NAME`/`MEDIA_MIME`/`MEDIA_SIZE`/
      `MESSAGE_DURATION`/`MESSAGE_STATUS` constants to `iped-parsers-common`'s
      `ConversationConstants` (same string values — zero behavior change) and switched
      all four parsers' `metadata.set/add` call sites to reference them. DB-column-name
      literals (e.g. `rs.getString("mediaName")` reading the source app's own SQLite
      schema) were intentionally left untouched — different concern, tied to on-disk
      schema, not index metadata naming. `SkypeParser`'s `"sendingStatus"` also left as
      a literal — it's Skype-specific, not a cross-parser convention.
- [x] Logging migration to `@Slf4j` already complete across existing split-out modules.

## Phase 3 — Architecture
- [~] Parsers consume only `iped-api` + Tika + `iped-parsers-common` — no engine imports
      (ArchUnit rule at the parent level applied to every child). Previously only
      `iped-parsers-impl`'s `ParsersBoundaryTest` enforced this — and only for the 16
      modules `iped-parsers-impl` happens to depend on (`apk`, `browsers`, `compression`,
      `database-edb`, `db-base`, `discord-cache`, `mp4-detector`, `plist-detector`,
      `sqlite-core`, `telegram`, `threema`, `tor`, `ufed`, `vcard`, `video`, `whatsapp`);
      16 other modules (`ares`, `bittorrent`, `eventtranscript`, `external`, `gdrive`,
      `emule`, `lnk`, `mail`, `registry`, `security`, `shareaza`, `skype`,
      `sqlite-detector`, `usnjrnl`, `vlc`, `winx`) had **zero** boundary enforcement —
      none of them declare an `iped-engine`/`iped-app` dependency today, but nothing
      would have caught it if one were added. Added a dedicated `XxxBoundaryTest`
      (same two-rule shape as `ParsersBoundaryTest`) to each of those 16 modules' own
      `src/test/java/iped/arch/`, scanning only that module's own package — 32 tests,
      all green. The 16 modules covered transitively via `iped-parsers-impl` are left
      as-is (genuinely redundant to duplicate); marked `[~]` rather than `[x]` because
      that transitive coverage is still fragile to `iped-parsers-impl`'s dependency list
      changing — a true parent-level rule (one test, runs against every child module's
      output) would need a multi-module ArchUnit test setup, not yet built.
- [ ] Decouple parser HTML report generation from Swing/viewer assumptions so output
      renders identically in the browser UI (5.0 workstream 1).
- [ ] External-process parser isolation (`iped-parser-external` pattern): evaluate
      sandboxing/timeout hardening for crash-prone native parsers.

## Phase 4 — 5.0
- [ ] Parser execution as distributed work units: parsers must be stateless per-item or
      declare their state requirements (needed by `iped-distributed` work-unit model).
- [ ] Per-parser docs page (what it extracts, properties emitted) generated into the
      scripting/API documentation set.

## Progress checks
- `mvn -pl iped-parsers -am verify` green; fixture corpus green.
- No parser module imports `iped.engine.*` (ArchUnit).
