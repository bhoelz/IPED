package iped.arch;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.lang.ArchRule;
import org.junit.jupiter.api.Test;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

/**
 * Enforces that iped-parser-mail has no dependency on the engine or
 * application layers. Parsers are Tika extensions and must be portable.
 */
class MailBoundaryTest {

    private static final JavaClasses PARSER_CLASSES = new ClassFileImporter()
            .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
            .importPackages("iped.parsers.mail");

    @Test
    void parsersMustNotImportEngine() {
        ArchRule rule = noClasses()
                .that().resideInAnyPackage("iped.parsers.mail..")
                .should().dependOnClassesThat()
                .resideInAPackage("iped.engine..")
                .because("parsers are Tika extensions and must be usable without the IPED engine; " +
                         "engine-specific behaviour belongs in task wrappers");
        rule.check(PARSER_CLASSES);
    }

    @Test
    void parsersMustNotImportAppLayer() {
        ArchRule rule = noClasses()
                .that().resideInAnyPackage("iped.parsers.mail..")
                .should().dependOnClassesThat()
                .resideInAPackage("iped.app..")
                .because("parsers must be headless and UI-independent");
        rule.check(PARSER_CLASSES);
    }
}
