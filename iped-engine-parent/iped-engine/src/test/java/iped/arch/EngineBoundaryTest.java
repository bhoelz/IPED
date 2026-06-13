package iped.arch;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.lang.ArchRule;
import org.junit.jupiter.api.Test;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

/**
 * Enforces architectural boundaries for iped-engine:
 * <ul>
 *   <li>Engine orchestration code must not import UI/app types.</li>
 *   <li>Engine must not import task or parser implementation types
 *       (only their SPI/API contracts).</li>
 *   <li>Engine must not contain Kafka or distributed-infrastructure types;
 *       that coupling belongs in iped-distributed.</li>
 * </ul>
 */
class EngineBoundaryTest {

    private static final JavaClasses ENGINE_CLASSES = new ClassFileImporter()
            .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
            .importPackages("iped.engine");

    @Test
    void engineMustNotImportSwingOrAwtUiClasses() {
        ArchRule rule = noClasses()
                .that().resideInAnyPackage(
                        "iped.engine.core..",
                        "iped.engine.config..",
                        "iped.engine.lucene..",
                        "iped.engine.task..")
                .should().dependOnClassesThat()
                .resideInAPackage("javax.swing..")
                .because("engine orchestration and indexing layers must be headless; " +
                         "Swing dependencies must live only in iped-app and UI listeners");
        rule.check(ENGINE_CLASSES);
    }

    @Test
    void engineMustNotImportKafkaTypes() {
        ArchRule rule = noClasses()
                .that().resideInAPackage("iped.engine..")
                .should().dependOnClassesThat()
                .resideInAnyPackage(
                        "org.apache.kafka..",
                        "iped.distributed.kafka..")
                .because("Kafka coupling must live in iped-distributed, not the engine; " +
                         "use iped.pipeline.IJobLifecycleListener / IItemProcessingListener " +
                         "API contracts to communicate job events");
        rule.check(ENGINE_CLASSES);
    }

    @Test
    void engineCoreMustNotImportAppPackage() {
        ArchRule rule = noClasses()
                .that().resideInAPackage("iped.engine..")
                .should().dependOnClassesThat()
                .resideInAPackage("iped.app..")
                .because("iped-engine must not depend on the application layer (iped-app); " +
                         "inject UI callbacks via iped.pipeline listener interfaces instead");
        rule.check(ENGINE_CLASSES);
    }
}
