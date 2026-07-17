package iped.arch;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.lang.ArchRule;
import org.junit.jupiter.api.Test;

/**
 * Verifies the parser boundary against the complete parser runtime classpath.
 *
 * <p>This test intentionally lives in {@code iped-parsers-main}, which aggregates every parser
 * child module. The module-local boundary tests remain useful for fast feedback, while this test
 * prevents a newly added child module from escaping the architecture gate merely because it is
 * not yet a transitive dependency of {@code iped-parsers-impl}.
 */
class AllParsersBoundaryTest {

  private static final JavaClasses ALL_PARSER_CLASSES =
      new ClassFileImporter()
          .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
          .importPackages("iped.parsers");

  @Test
  void allParserModulesMustNotImportEngine() {
    ArchRule rule =
        noClasses()
            .that()
            .resideInAnyPackage("iped.parsers..")
            .should()
            .dependOnClassesThat()
            .resideInAnyPackage("iped.engine..")
            .because(
                "all parser child modules must remain portable and independent from the engine");
    rule.check(ALL_PARSER_CLASSES);
  }

  @Test
  void allParserModulesMustNotImportAppLayer() {
    ArchRule rule =
        noClasses()
            .that()
            .resideInAnyPackage("iped.parsers..")
            .should()
            .dependOnClassesThat()
            .resideInAnyPackage("iped.app..")
            .because("all parser child modules must remain headless and UI-independent");
    rule.check(ALL_PARSER_CLASSES);
  }
}
