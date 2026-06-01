package iped.engine.core;

import static org.junit.jupiter.api.Assertions.*;

import java.io.File;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import iped.engine.config.ConfigurationView;
import iped.engine.data.CaseData;

/**
 * Tests verifying ThreadLocal isolation of CaseContext across threads
 * and correct set/clear lifecycle.
 */
public class ThreadLocalContextTest {

    @BeforeEach
    void setUp() {
        CaseContextThreadLocal.clear();
    }

    @AfterEach
    void tearDown() {
        CaseContextThreadLocal.clear();
    }

    @Test
    void testSetAndGetContext() {
        UUID caseId = UUID.randomUUID();
        CaseContext context = new CaseContext.Builder(caseId)
                .withCaseData(new CaseData())
                .withConfigurationView(new ConfigurationView())
                .build();

        CaseContextThreadLocal.set(context);
        CaseContext retrieved = CaseContextThreadLocal.get();

        assertNotNull(retrieved, "Context should be retrievable");
        assertEquals(caseId, retrieved.getId(), "Retrieved context should match set context");
    }

    @Test
    void testClearContext() {
        UUID caseId = UUID.randomUUID();
        CaseContext context = new CaseContext.Builder(caseId)
                .withCaseData(new CaseData())
                .withConfigurationView(new ConfigurationView())
                .build();

        CaseContextThreadLocal.set(context);
        CaseContextThreadLocal.clear();

        assertNull(CaseContextThreadLocal.get(), "Context should be null after clear");
    }

    @Test
    void testIsolationAcrossThreads() throws InterruptedException {
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

        AtomicReference<UUID> thread1ContextId = new AtomicReference<>();
        AtomicReference<UUID> thread2ContextId = new AtomicReference<>();
        CountDownLatch latch = new CountDownLatch(2);

        Thread t1 = new Thread(() -> {
            CaseContextThreadLocal.set(context1);
            thread1ContextId.set(CaseContextThreadLocal.get().getId());
            latch.countDown();
        });

        Thread t2 = new Thread(() -> {
            CaseContextThreadLocal.set(context2);
            thread2ContextId.set(CaseContextThreadLocal.get().getId());
            latch.countDown();
        });

        t1.start();
        t2.start();
        latch.await();

        assertEquals(case1Id, thread1ContextId.get(),
                "Thread 1 should have case1 context");
        assertEquals(case2Id, thread2ContextId.get(),
                "Thread 2 should have case2 context");
    }

    @Test
    void testContextIsolationNoLeakBetweenThreads() throws InterruptedException {
        UUID case1Id = UUID.randomUUID();
        CaseContext context1 = new CaseContext.Builder(case1Id)
                .withCaseData(new CaseData())
                .withConfigurationView(new ConfigurationView())
                .build();

        AtomicReference<CaseContext> thread1InitialContext = new AtomicReference<>();
        AtomicReference<CaseContext> thread2InitialContext = new AtomicReference<>();
        CountDownLatch latch = new CountDownLatch(2);

        Thread t1 = new Thread(() -> {
            CaseContextThreadLocal.set(context1);
            latch.countDown();
        });

        Thread t2 = new Thread(() -> {
            thread2InitialContext.set(CaseContextThreadLocal.get());
            latch.countDown();
        });

        t1.start();
        t2.start();
        latch.await();

        // Thread 2 should not see Thread 1's context
        assertNull(thread2InitialContext.get(),
                "Thread 2 should not have access to Thread 1's context");
    }

    @Test
    void testMultipleSetAndGetCycles() {
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

        // Set context 1
        CaseContextThreadLocal.set(context1);
        assertEquals(case1Id, CaseContextThreadLocal.get().getId());

        // Switch to context 2
        CaseContextThreadLocal.set(context2);
        assertEquals(case2Id, CaseContextThreadLocal.get().getId());

        // Switch back to context 1
        CaseContextThreadLocal.set(context1);
        assertEquals(case1Id, CaseContextThreadLocal.get().getId());

        CaseContextThreadLocal.clear();
        assertNull(CaseContextThreadLocal.get());
    }

    @Disabled("Manager constructor requires ConfigurationManager to be initialized — integration test only")
    @Test
    void testManagerInstanceDelegation() {
        UUID caseId = UUID.randomUUID();
        java.util.List<File> sources = new java.util.ArrayList<>();
        Manager manager = new Manager(sources, new File("/tmp/output"), new File("/tmp/palavras"));
        CaseContext context = new CaseContext.Builder(caseId)
                .withCaseData(new CaseData())
                .withConfigurationView(new ConfigurationView())
                .withManager(manager)
                .build();

        CaseContextThreadLocal.set(context);

        // Manager.getInstance() should delegate through ThreadLocal
        Manager retrievedManager = Manager.getInstance();
        assertEquals(manager, retrievedManager,
                "Manager.getInstance() should return ThreadLocal manager");

        CaseContextThreadLocal.clear();
    }
}
