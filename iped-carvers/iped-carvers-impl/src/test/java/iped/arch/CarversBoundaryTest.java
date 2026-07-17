package iped.arch;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.lang.ArchRule;
import org.junit.jupiter.api.Test;

/**
 * Enforces that iped-carvers-impl has no upward dependency on the engine or application layers.
 * Carvers must be self-contained and reusable outside IPED.
 */
class CarversBoundaryTest {

  private static final JavaClasses CARVER_CLASSES =
      new ClassFileImporter()
          .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
          .importPackages("iped.carvers");

  @Test
  void carversMustNotImportEngine() {
    ArchRule rule =
        noClasses()
            .that()
            .resideInAnyPackage("iped.carvers..")
            .should()
            .dependOnClassesThat()
            .resideInAPackage("iped.engine..")
            .because(
                "carvers are a standalone library — engine orchestration belongs in "
                    + "iped-tasks-carving, not in the carver implementations themselves");
    rule.check(CARVER_CLASSES);
  }

  @Test
  void carversMustNotImportAppLayer() {
    ArchRule rule =
        noClasses()
            .that()
            .resideInAnyPackage("iped.carvers..")
            .should()
            .dependOnClassesThat()
            .resideInAPackage("iped.app..")
            .because("carvers must be headless and UI-independent");
    rule.check(CARVER_CLASSES);
  }
}
