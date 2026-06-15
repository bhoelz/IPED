package iped.arch;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.lang.ArchRule;
import org.junit.jupiter.api.Test;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

/**
 * Enforces that iped-tasks.spi depends only on iped-api — it must never pull
 * in engine internals, task implementations, parsers, or the app layer.
 */
class TaskSpiArchTest {

    private static final JavaClasses SPI_CLASSES = new ClassFileImporter()
            .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
            .importPackages("iped.tasks.spi");

    @Test
    void spiMustNotImportEngineInternals() {
        ArchRule rule = noClasses()
                .that().resideInAPackage("iped.tasks.spi..")
                .should().dependOnClassesThat()
                .resideInAnyPackage("iped.engine..")
                .because("the task SPI is a public extension point — it must depend only on " +
                         "iped-api so third-party plugins can implement it without pulling in engine internals");
        rule.check(SPI_CLASSES);
    }

    @Test
    void spiMustNotImportAppLayer() {
        ArchRule rule = noClasses()
                .that().resideInAPackage("iped.tasks.spi..")
                .should().dependOnClassesThat()
                .resideInAPackage("iped.app..")
                .because("the task SPI must be headless and UI-independent");
        rule.check(SPI_CLASSES);
    }

    @Test
    void spiMustNotImportParserImpl() {
        ArchRule rule = noClasses()
                .that().resideInAPackage("iped.tasks.spi..")
                .should().dependOnClassesThat()
                .resideInAnyPackage("iped.parsers..", "org.apache.tika.parser..")
                .because("the task SPI must not bind to parser implementations");
        rule.check(SPI_CLASSES);
    }
}
