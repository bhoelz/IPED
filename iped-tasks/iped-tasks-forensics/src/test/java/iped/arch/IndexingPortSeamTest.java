package iped.arch;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.EvaluationResult;
import org.junit.jupiter.api.Test;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * ADR 0001 (ports-and-adapters-indexing-seam): {@code DuplicateTask} is the
 * PI-1-F4a-S1 POC sample Task migrated to depend on {@code iped.index.spi.IndexingPort}
 * instead of Lucene directly. This guards the seam: the sample Task must not
 * reintroduce a dependency on {@code org.apache.lucene..}.
 *
 * <p>A negative test proves the rule has teeth by running it against a
 * fixture class ({@link iped.arch.fixture.LuceneOffendingTask}) that
 * deliberately violates it, in the {@code iped.arch.fixture} test-only
 * package, confirming the rule actually fails instead of trivially passing.
 */
class IndexingPortSeamTest {

    private static final ArchRule SAMPLE_TASK_MUST_NOT_DEPEND_ON_LUCENE = noClasses()
            .that().haveSimpleName("DuplicateTask")
            .should().dependOnClassesThat().resideInAPackage("org.apache.lucene..")
            .because("ADR 0001: DuplicateTask is migrated behind IndexingPort and must not "
                    + "depend on org.apache.lucene.. directly");

    @Test
    void duplicateTaskMustNotDependOnLuceneDirectly() {
        JavaClasses classes = new ClassFileImporter()
                .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
                .importPackages("iped.engine.task");
        SAMPLE_TASK_MUST_NOT_DEPEND_ON_LUCENE.check(classes);
    }

    /**
     * Negative test: run the same rule (adapted to the fixture's simple name)
     * against a fixture class that deliberately imports {@code org.apache.lucene..}
     * to prove the rule actually catches a violation, not just passes trivially.
     */
    @Test
    void ruleActuallyCatchesALuceneDependencyViolation() {
        ArchRule ruleAgainstFixture = noClasses()
                .that().haveSimpleName("LuceneOffendingTask")
                .should().dependOnClassesThat().resideInAPackage("org.apache.lucene..")
                .because("negative-test fixture: proves the seam-guard rule has teeth");

        // Note: no DO_NOT_INCLUDE_TESTS import option here — the fixture itself
        // lives under src/test/java and would otherwise be filtered out.
        JavaClasses fixtureClasses = new ClassFileImporter()
                .importPackages("iped.arch.fixture");

        EvaluationResult result = ruleAgainstFixture.evaluate(fixtureClasses);

        assertTrue(result.hasViolation(),
                "expected the ArchUnit rule to flag iped.arch.fixture.LuceneOffendingTask, "
                        + "which deliberately depends on org.apache.lucene..");
        assertFalse(result.getFailureReport().toString().isEmpty());
    }
}
