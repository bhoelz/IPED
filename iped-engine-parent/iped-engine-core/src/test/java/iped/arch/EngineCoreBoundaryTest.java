package iped.arch;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.lang.ArchRule;
import org.junit.jupiter.api.Test;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

/**
 * Ensures iped-engine-core does not depend on sibling modules within
 * iped-engine-parent. It may only depend on iped-api, iped-utils, and
 * third-party libraries.
 *
 * <p>Violations indicate logic that belongs in a higher layer (orchestration →
 * iped-engine) or a datasource-specific module.
 */
class EngineCoreBoundaryTest {

    private static final JavaClasses CORE_CLASSES = new ClassFileImporter()
            .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
            .importPackages("iped.engine");

    @Test
    void engineCoreMustNotImportSleuthkitTypes() {
        ArchRule rule = noClasses()
                .that().resideInAPackage("iped.engine..")
                .should().dependOnClassesThat()
                .resideInAPackage("org.sleuthkit..")
                .because("iped-engine-core must have zero datasource-specific imports; " +
                         "Sleuth Kit types belong in iped-sleuthkit");
        rule.check(CORE_CLASSES);
    }

    @Test
    void engineCoreMustNotImportUfedTypes() {
        ArchRule rule = noClasses()
                .that().resideInAPackage("iped.engine..")
                .should().dependOnClassesThat()
                .resideInAPackage("iped.parsers.ufed..")
                .because("iped-engine-core must have zero datasource-specific imports; " +
                         "UFED types belong in iped-ufed");
        rule.check(CORE_CLASSES);
    }

    @Test
    void engineCoreMustNotContainOrchestrationLogic() {
        ArchRule rule = noClasses()
                .that().resideInAPackage("iped.engine..")
                .and().haveSimpleNameContaining("Manager")
                .should().dependOnClassesThat()
                .resideInAPackage("org.apache.lucene..")
                .because("orchestration logic (Manager) and Lucene indexing belong in iped-engine, " +
                         "not in the shared iped-engine-core");
        rule.check(CORE_CLASSES);
    }
}
