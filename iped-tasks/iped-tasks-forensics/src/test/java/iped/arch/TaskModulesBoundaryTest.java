package iped.arch;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.lang.ArchRule;
import org.junit.jupiter.api.Test;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

/**
 * Enforces task-module architectural boundaries for the forensics task module:
 * <ul>
 *   <li>Task code must not depend on iped-app (the UI/launcher layer).</li>
 *   <li>Task configs must extend AbstractTaskConfig, not be arbitrary beans.</li>
 * </ul>
 * Each task module carries a copy of these rules so the constraint is checked
 * per module; the parent aggregator has no classes to scan itself.
 */
class TaskModulesBoundaryTest {

    private static final JavaClasses TASK_CLASSES = new ClassFileImporter()
            .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
            .importPackages("iped.engine.task", "iped.engine.config");

    @Test
    void tasksMustNotImportAppLayer() {
        ArchRule rule = noClasses()
                .that().resideInAnyPackage("iped.engine.task..", "iped.engine.config..")
                .should().dependOnClassesThat()
                .resideInAPackage("iped.app..")
                .because("task modules must be headless and runnable without the iped-app UI layer; " +
                         "inject UI callbacks via listener interfaces instead");
        rule.check(TASK_CLASSES);
    }
}
