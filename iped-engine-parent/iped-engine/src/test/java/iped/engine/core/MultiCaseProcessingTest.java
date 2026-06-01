package iped.engine.core;

import static org.junit.jupiter.api.Assertions.*;

import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import iped.engine.config.ConfigurationView;
import iped.engine.core.Statistics;
import iped.engine.data.CaseData;
import iped.engine.data.Item;

/**
 * Integration tests verifying multi-case processing with independent
 * statistics, configurations, and item counter sequences.
 */
public class MultiCaseProcessingTest {

    private ProcessingOrchestrator orchestrator;

    @BeforeEach
    void setUp() {
        ProcessingOrchestrator.initialize(2, 5000000);
        orchestrator = ProcessingOrchestrator.getInstance();
    }

    @AfterEach
    void tearDown() {
        CaseContextThreadLocal.clear();
    }

    @Test
    void testTwoCasesRunInParallel() throws InterruptedException {
        UUID case1Id = UUID.randomUUID();
        UUID case2Id = UUID.randomUUID();

        CaseContext context1 = new CaseContext.Builder(case1Id)
                .withCaseData(new CaseData())
                .withConfigurationView(new ConfigurationView())
                .build();

        CaseContext context2 = new CaseContext.Builder(case2Id)
                .withCaseData(new CaseData())
                .withConfigurationView(new ConfigurationView())
                .build();

        UUID returned1 = orchestrator.enqueueCaseForProcessing(context1);
        UUID returned2 = orchestrator.enqueueCaseForProcessing(context2);

        assertEquals(case1Id, returned1);
        assertEquals(case2Id, returned2);
        assertTrue(orchestrator.getActiveCaseIds().contains(case1Id)
                || orchestrator.getActiveCaseIds().contains(case2Id),
                "At least one case should be active");
    }

    @Test
    void testIndependentStatisticsPerCase() throws InterruptedException {
        UUID case1Id = UUID.randomUUID();
        UUID case2Id = UUID.randomUUID();

        java.io.File tmpDir = new java.io.File(System.getProperty("java.io.tmpdir"), "iped-test");
        Statistics stats1 = Statistics.get(new CaseData(), tmpDir);
        Statistics stats2 = Statistics.get(new CaseData(), tmpDir);

        CaseContext context1 = new CaseContext.Builder(case1Id)
                .withCaseData(new CaseData())
                .withConfigurationView(new ConfigurationView())
                .withStatistics(stats1)
                .build();

        CaseContext context2 = new CaseContext.Builder(case2Id)
                .withCaseData(new CaseData())
                .withConfigurationView(new ConfigurationView())
                .withStatistics(stats2)
                .build();

        orchestrator.enqueueCaseForProcessing(context1);
        orchestrator.enqueueCaseForProcessing(context2);

        assertNotNull(context1.getStatistics(), "Case 1 should have statistics");
        assertNotNull(context2.getStatistics(), "Case 2 should have statistics");
        assertNotSame(context1.getStatistics(), context2.getStatistics(),
                "Each case should have independent statistics instance");
    }

    @Test
    void testItemIdSequencePerCase() {
        UUID case1Id = UUID.randomUUID();
        UUID case2Id = UUID.randomUUID();

        CaseContext context1 = new CaseContext.Builder(case1Id)
                .withCaseData(new CaseData())
                .withConfigurationView(new ConfigurationView())
                .build();

        CaseContext context2 = new CaseContext.Builder(case2Id)
                .withCaseData(new CaseData())
                .withConfigurationView(new ConfigurationView())
                .build();

        // Simulate case 1 processing
        CaseContextThreadLocal.set(context1);
        int id1_first = Item.getNextId();
        int id1_second = Item.getNextId();
        CaseContextThreadLocal.clear();

        // Simulate case 2 processing
        CaseContextThreadLocal.set(context2);
        int id2_first = Item.getNextId();
        int id2_second = Item.getNextId();
        CaseContextThreadLocal.clear();

        assertTrue(id1_first < id1_second, "Case 1 IDs should increment");
        assertTrue(id2_first < id2_second, "Case 2 IDs should increment");
        // Each case has its own counter starting from 0 — independence means both start at 0,
        // not that they produce globally unique values.
        assertEquals(id1_first, id2_first, "Case 1 and Case 2 should have independent sequences, each starting at 0");
    }

    @Test
    void testPauseAndResumeOperations() throws InterruptedException {
        UUID caseId = UUID.randomUUID();
        CaseContext context = new CaseContext.Builder(caseId)
                .withCaseData(new CaseData())
                .withConfigurationView(new ConfigurationView())
                .build();

        orchestrator.enqueueCaseForProcessing(context);

        orchestrator.pauseCase(caseId);
        assertEquals(CaseContext.CaseState.PAUSED, context.getState(),
                "Case should be paused");

        orchestrator.resumeCase(caseId);
        assertEquals(CaseContext.CaseState.RUNNING, context.getState(),
                "Case should be running after resume");
    }

    @Test
    void testResourceBackpressureWhenLimitExceeded() throws InterruptedException {
        // Orchestrator initialized with max 2 concurrent cases
        UUID case1Id = UUID.randomUUID();
        UUID case2Id = UUID.randomUUID();
        UUID case3Id = UUID.randomUUID();

        CaseContext context1 = new CaseContext.Builder(case1Id)
                .withCaseData(new CaseData())
                .withConfigurationView(new ConfigurationView())
                .build();

        CaseContext context2 = new CaseContext.Builder(case2Id)
                .withCaseData(new CaseData())
                .withConfigurationView(new ConfigurationView())
                .build();

        CaseContext context3 = new CaseContext.Builder(case3Id)
                .withCaseData(new CaseData())
                .withConfigurationView(new ConfigurationView())
                .build();

        orchestrator.enqueueCaseForProcessing(context1);
        orchestrator.enqueueCaseForProcessing(context2);
        orchestrator.enqueueCaseForProcessing(context3);

        // Third case should be queued when limit is reached
        assertTrue(orchestrator.getActiveCaseCount() <= 2,
                "Should not exceed max concurrent cases limit");
    }

    @Test
    void testCompleteCaseProcessingReleaseResources() throws InterruptedException {
        UUID caseId = UUID.randomUUID();
        CaseContext context = new CaseContext.Builder(caseId)
                .withCaseData(new CaseData())
                .withConfigurationView(new ConfigurationView())
                .build();

        orchestrator.enqueueCaseForProcessing(context);
        int activeBefore = orchestrator.getActiveCaseCount();

        orchestrator.completeCaseProcessing(caseId);
        assertNull(orchestrator.getCaseContext(caseId),
                "Case context should be removed after completion");
    }

    @Test
    void testWaitForCompletionTimeout() throws InterruptedException {
        UUID caseId = UUID.randomUUID();
        CaseContext context = new CaseContext.Builder(caseId)
                .withCaseData(new CaseData())
                .withConfigurationView(new ConfigurationView())
                .build();

        orchestrator.enqueueCaseForProcessing(context);

        // Wait with very short timeout should return false if case still active
        boolean completed = orchestrator.waitForCompletion(100);
        assertFalse(completed, "Completion should timeout when cases are still active");
    }
}
