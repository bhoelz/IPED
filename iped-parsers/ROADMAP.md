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
- [ ] Per-module dependency hygiene: each parser declares only what it uses; JDBC drivers
      at `runtime` scope; remove zero-usage deps (see commons-lang 2.6 in registry/skype).
- [x] ArchUnit guard added: `ParsersBoundaryTest` in `iped-parsers-impl` enforces that
      no parser class imports `iped.engine.*` or `iped.app.*`.
- [ ] Define the parser plugin contract formally (manifest, supported MIME types,
      ordering/priority) so third-party parser plugins are feasible without forking.

## Phase 2 — Quality and coverage
- [ ] Fixture-based regression corpus per parser module (small real-world samples, run in
      CI) — protects against Tika upgrades and refactors.
- [ ] Tika upgrade policy: track releases, run the full fixture corpus as the gate.
- [ ] Standardize metadata property naming across parsers (audit drift against
      `iped.properties` definitions in `iped-api`).
- [x] Logging migration to `@Slf4j` already complete across existing split-out modules.

## Phase 3 — Architecture
- [ ] Parsers consume only `iped-api` + Tika + `iped-parsers-common` — no engine imports
      (ArchUnit rule at the parent level applied to every child).
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
