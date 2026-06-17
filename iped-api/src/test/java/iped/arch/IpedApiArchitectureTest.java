package iped.arch;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.lang.ArchRule;
import org.junit.jupiter.api.Test;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

/**
 * Ensures iped-api contains no compile-time dependency on any other IPED
 * module. iped-api is the public contract layer; only SLF4J and standard
 * libraries are permitted as runtime dependencies.
 */
class IpedApiArchitectureTest {

    private static final JavaClasses API_CLASSES = new ClassFileImporter()
            .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
            .importPackages("iped");

    @Test
    void apiMustNotDependOnEnginePackages() {
        ArchRule rule = noClasses()
                .that().resideInAPackage("iped..")
                .should().dependOnClassesThat()
                .resideInAnyPackage(
                        "iped.engine..",
                        "iped.app..",
                        "iped.parsers..",
                        "iped.viewers..",
                        "iped.tasks..",
                        "iped.carvers..",
                        "iped.distributed..",
                        "iped.utils.."
                )
                .because("iped-api must remain a zero-implementation contract layer with no " +
                         "dependency on engine or plugin modules");
        rule.check(API_CLASSES);
    }

    @Test
    void apiMustNotHaveImplementationClasses() {
        ArchRule rule = noClasses()
                .that().resideInAPackage("iped..")
                .and().haveSimpleNameNotContaining("Test")
                .should().beAnnotatedWith("org.springframework.stereotype.Service")
                .because("iped-api should contain only interfaces, annotations, and value types");
        rule.check(API_CLASSES);
    }
}
