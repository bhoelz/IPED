package iped.engine.arch;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

/**
 * Architecture rules for the iped-engine-core module.
 *
 * <p>Enforces that engine internals do not leak upward into the application or
 * viewers-implementation layers, which would create hidden coupling and make headless/server
 * deployments impossible.
 */
@AnalyzeClasses(packages = "iped.engine", importOptions = ImportOption.DoNotIncludeTests.class)
public class EngineCoreArchTest {

  /**
   * The engine must never reach into {@code iped.app}. Application-layer classes (Swing UI, desktop
   * bootstrapping) cannot be on the classpath in server mode.
   */
  @ArchTest
  static final ArchRule engine_must_not_depend_on_app =
      noClasses()
          .should()
          .dependOnClassesThat()
          .resideInAPackage("iped.app..")
          .because("the engine layer must be deployable without the desktop application layer");

  /**
   * Engine classes may use the viewers <em>API</em> but must not bind to concrete viewer
   * implementations. Binding to {@code iped.viewers.impl} would prevent swapping or omitting the
   * viewer stack in headless deployments.
   */
  @ArchTest
  static final ArchRule engine_must_not_depend_on_viewers_impl =
      noClasses()
          .should()
          .dependOnClassesThat()
          .resideInAPackage("iped.viewers.impl..")
          .because("engine code must program to the viewers API, not the implementation");
}
