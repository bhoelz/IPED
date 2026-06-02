package iped.engine.core;

import iped.engine.config.ConfigurationView;
import iped.engine.data.CaseData;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests to verify that Worker correctly manages ThreadLocal CaseContext.
 *
 * These tests verify the ThreadLocal setup behavior and isolation pattern
 * used by Worker to ensure each worker thread has its own case context.
 */
public class WorkerThreadContextTest {

    @AfterEach
    void tearDown() {
        CaseContextThreadLocal.clear();
    }

    @Test
    void testContextSetupAndCleanup() {
        UUID caseId = UUID.randomUUID();
        CaseContext context = new CaseContext.Builder(caseId)
                .withManager(null)
                .withCaseData(new CaseData())
                .withConfigurationView(new ConfigurationView())
                .build();

        // Verify context can be set
        CaseContextThreadLocal.set(context);
        assertNotNull(CaseContextThreadLocal.get(), "Context should be set");
        assertEquals(caseId, CaseContextThreadLocal.get().getId(), "Should have correct case ID");

        // Verify cleanup
        CaseContextThreadLocal.clear();
        assertNull(CaseContextThreadLocal.get(), "Context should be cleared");
    }

    @Test
    void testMainThreadContextNotAffectedByWorkerThread() throws InterruptedException {
        UUID mainCaseId = UUID.randomUUID();
        UUID workerCaseId = UUID.randomUUID();

        CaseContext context1 = new CaseContext.Builder(mainCaseId)
                .withManager(null)
                .withCaseData(new CaseData())
                .withConfigurationView(new ConfigurationView())
                .build();

        CaseContext context2 = new CaseContext.Builder(workerCaseId)
                .withManager(null)
                .withCaseData(new CaseData())
                .withConfigurationView(new ConfigurationView())
                .build();

        // Main thread sets context1
        CaseContextThreadLocal.set(context1);

        // Simulate worker thread setting and clearing context (like Worker.run() does)
        UUID[] workerSawContext = { null };
        Thread workerThread = new Thread(() -> {
            CaseContextThreadLocal.set(context2);
            workerSawContext[0] = CaseContextThreadLocal.get().getId();
            // Simulate finally block in Worker.run()
            CaseContextThreadLocal.clear();
        });

        workerThread.start();
        workerThread.join();

        // Main thread should still have context1
        assertEquals(mainCaseId, CaseContextThreadLocal.get().getId(),
                "Main thread should still have its original context");
        // Verify worker saw its own context
        assertEquals(workerCaseId, workerSawContext[0], "Worker should have seen its context");
    }

    @Test
    void testMultipleWorkersWithDifferentContexts() throws InterruptedException {
        UUID case1Id = UUID.randomUUID();
        UUID case2Id = UUID.randomUUID();

        CaseContext context1 = new CaseContext.Builder(case1Id)
                .withManager(null)
                .withCaseData(new CaseData())
                .withConfigurationView(new ConfigurationView())
                .build();

        CaseContext context2 = new CaseContext.Builder(case2Id)
                .withManager(null)
                .withCaseData(new CaseData())
                .withConfigurationView(new ConfigurationView())
                .build();

        UUID[] results = new UUID[2];

        Thread worker1 = new Thread(() -> {
            try {
                CaseContextThreadLocal.set(context1);
                results[0] = CaseContextThreadLocal.get().getId();
            } finally {
                CaseContextThreadLocal.clear();
            }
        });

        Thread worker2 = new Thread(() -> {
            try {
                CaseContextThreadLocal.set(context2);
                results[1] = CaseContextThreadLocal.get().getId();
            } finally {
                CaseContextThreadLocal.clear();
            }
        });

        worker1.start();
        worker2.start();
        worker1.join();
        worker2.join();

        assertEquals(case1Id, results[0], "Worker 1 should see case 1 context");
        assertEquals(case2Id, results[1], "Worker 2 should see case 2 context");
    }
}
