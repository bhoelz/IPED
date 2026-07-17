package iped.arch;

import static com.tngtech.archunit.core.domain.JavaClass.Predicates.resideInAPackage;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.library.dependencies.SlicesRuleDefinition.slices;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

/**
 * Cross-module architecture rules enforced from the iped-engine aggregate, which has the widest
 * compile-time view of the IPED module graph (api, utils, engine, parsers, carvers, tasks-spi,
 * viewers).
 *
 * <p>Rules are grouped by layer:
 *
 * <ol>
 *   <li><b>API / foundation packages</b> — must not reach upward
 *   <li><b>Service provider interfaces (tasks-spi, carvers-api)</b> — must not depend on engine
 *   <li><b>Parsers</b> — must not depend on engine or app
 *   <li><b>Engine</b> — must not depend on app
 *   <li><b>No package-level cycles</b>
 * </ol>
 */
@AnalyzeClasses(packages = "iped", importOptions = ImportOption.DoNotIncludeTests.class)
public class CrossModuleArchTest {

  // -------------------------------------------------------------------------
  // 1. API / foundation packages (iped-api module packages)
  // -------------------------------------------------------------------------

  /**
   * The core data/API packages form the foundation of the module hierarchy. They must not import
   * from implementation or application layers.
   */
  @ArchTest
  static final ArchRule api_packages_must_not_depend_on_implementations =
      noClasses()
          .that()
          .resideInAnyPackage(
              "iped.data..",
              "iped.datasource..",
              "iped.configuration..",
              "iped.exception..",
              "iped.io..",
              "iped.properties..",
              "iped.search..",
              "iped.localization..")
          .should()
          .dependOnClassesThat()
          .resideInAnyPackage(
              "iped.engine..",
              "iped.app..",
              "iped.parsers..",
              "iped.carvers..",
              "iped.tasks..",
              "iped.viewers..")
          .because(
              "iped-api packages are the dependency-free foundation; nothing may pull them downward into implementations");

  // -------------------------------------------------------------------------
  // 2. Service provider interfaces
  // -------------------------------------------------------------------------

  /**
   * {@code iped-tasks.spi} is a pure service-provider contract. It may only reference the core API
   * (iped.api packages) — never engine internals — so that task providers can be implemented and
   * tested without the full engine.
   */
  @ArchTest
  static final ArchRule tasks_spi_must_not_depend_on_engine =
      noClasses()
          .that()
          .resideInAPackage("iped.tasks.spi..")
          .should()
          .dependOnClassesThat()
          .resideInAPackage("iped.engine..")
          .because(
              "iped-tasks.spi is a service-provider interface; coupling it to the engine prevents standalone task implementations");

  /**
   * {@code iped-carvers-api} declares the carver contract. It must stay free of engine internals
   * for the same reason as the tasks SPI.
   */
  @ArchTest
  static final ArchRule carvers_api_must_not_depend_on_engine =
      noClasses()
          .that()
          .resideInAPackage("iped.carvers.api..")
          .should()
          .dependOnClassesThat()
          .resideInAPackage("iped.engine..")
          .because(
              "iped-carvers-api is a service interface and must not depend on engine implementation details");

  // -------------------------------------------------------------------------
  // 3. Parsers
  // -------------------------------------------------------------------------

  /**
   * Parsers deal with file formats and must be reusable outside of IPED (e.g. as standalone Tika
   * parsers). They must not import engine internals or application-layer classes.
   */
  @ArchTest
  static final ArchRule parsers_must_not_depend_on_engine =
      noClasses()
          .that()
          .resideInAPackage("iped.parsers..")
          .should()
          .dependOnClassesThat()
          .resideInAPackage("iped.engine..")
          .because(
              "parsers must be reusable without the engine; depending on iped.engine creates a cycle (engine-core depends on parsers-impl)");

  @ArchTest
  static final ArchRule parsers_must_not_depend_on_app =
      noClasses()
          .that()
          .resideInAPackage("iped.parsers..")
          .should()
          .dependOnClassesThat()
          .resideInAPackage("iped.app..")
          .because(
              "parsers are a format-processing layer and must not depend on the desktop application");

  // -------------------------------------------------------------------------
  // 4. Engine
  // -------------------------------------------------------------------------

  /**
   * The engine layer is the processing core and must remain deployable in headless / server mode,
   * where no desktop application classes are present.
   */
  @ArchTest
  static final ArchRule engine_must_not_depend_on_app =
      noClasses()
          .that()
          .resideInAPackage("iped.engine..")
          .should()
          .dependOnClassesThat()
          .resideInAPackage("iped.app..")
          .because("the engine must be deployable without the iped-app desktop layer");

  // -------------------------------------------------------------------------
  // 5. No package-level cycles
  // -------------------------------------------------------------------------

  /**
   * Each first-level sub-package of {@code iped} (e.g. {@code iped.engine}, {@code iped.parsers},
   * {@code iped.carvers}) forms a slice. Cycles between slices would make the build order
   * indeterminate and prevent independent release of modules.
   *
   * <p><b>Known accepted cycle:</b> {@code iped.data} &harr; {@code iped.search} (both inside the
   * iped-api module, so the Maven build DAG is unaffected). {@link iped.data.IBookmarks} / {@link
   * iped.data.IMultiBookmarks} expose {@code filter*} methods over {@link iped.search.SearchResult}
   * / {@code IMultiSearchResult}, while {@code iped.search} legitimately depends on the core data
   * model ({@code IItemId}, {@code IIPEDSource}, {@code IItemReader}). Breaking it would require
   * either relocating the search-result types into {@code iped.data} (semantically wrong, ~40
   * dependents) or removing the filter methods from the public bookmarks API (breaks app/engine/geo
   * callers), so the {@code data -> search} edge is ignored explicitly here instead.
   *
   * <p><b>Known accepted cycle:</b> {@code iped.data} &harr; {@code iped.datasource} (both inside
   * the iped-api module, so the Maven build DAG is unaffected). {@link
   * iped.data.IItem#setDataSource(iped.datasource.IDataSource)} is the item&rarr;source
   * back-reference, while {@code iped.datasource.IDataSourceReader#iterator()} must yield {@code
   * Iterator<IItem>}. The edge is semantically necessary in both directions, so the {@code data ->
   * datasource} edge is ignored explicitly here instead.
   */
  @ArchTest
  static final ArchRule no_cycles_between_top_level_iped_packages =
      slices()
          .matching("iped.(*)..")
          .should()
          .beFreeOfCycles()
          .ignoreDependency(resideInAPackage("iped.data.."), resideInAPackage("iped.search.."))
          .ignoreDependency(resideInAPackage("iped.data.."), resideInAPackage("iped.datasource.."))
          .because(
              "circular dependencies between top-level iped packages break the module DAG and prevent independent releases");
}
