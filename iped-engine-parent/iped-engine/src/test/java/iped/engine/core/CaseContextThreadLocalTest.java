package iped.engine.core;

import iped.engine.config.ConfigurationView;
import iped.engine.data.CaseData;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

public class CaseContextThreadLocalTest {

    @AfterEach
    void tearDown() {
        CaseContextThreadLocal.clear();
    }

    private CaseContext createTestContext(UUID caseId) {
        return new CaseContext.Builder(caseId)
                .withCaseData(new CaseData())
                .withConfigurationView(new ConfigurationView())
                .build();
    }

    @Test
    void testSetAndGet() {
        assertNull(CaseContextThreadLocal.get(), "Should be null initially");

        UUID caseId = UUID.randomUUID();
        CaseContext context = createTestContext(caseId);

        CaseContextThreadLocal.set(context);
        assertEquals(context, CaseContextThreadLocal.get(), "Should return set context");
        assertEquals(caseId, CaseContextThreadLocal.get().getId(), "Should have correct case ID");
    }

    @Test
    void testClear() {
        UUID caseId = UUID.randomUUID();
        CaseContext context = createTestContext(caseId);

        CaseContextThreadLocal.set(context);
        assertNotNull(CaseContextThreadLocal.get(), "Should be set");

        CaseContextThreadLocal.clear();
        assertNull(CaseContextThreadLocal.get(), "Should be null after clear");
    }

    @Test
    void testIsSet() {
        assertFalse(CaseContextThreadLocal.isSet(), "Should not be set initially");

        UUID caseId = UUID.randomUUID();
        CaseContext context = createTestContext(caseId);
        CaseContextThreadLocal.set(context);

        assertTrue(CaseContextThreadLocal.isSet(), "Should be set");

        CaseContextThreadLocal.clear();
        assertFalse(CaseContextThreadLocal.isSet(), "Should not be set after clear");
    }

    @Test
    void testIsolationBetweenThreads() throws InterruptedException {
        UUID case1Id = UUID.randomUUID();
        UUID case2Id = UUID.randomUUID();

        CaseContext case1Context = createTestContext(case1Id);
        CaseContext case2Context = createTestContext(case2Id);

        CaseContextThreadLocal.set(case1Context);

        // Thread 2 should have its own context
        UUID[] thread2Result = new UUID[1];
        Thread thread2 = new Thread(() -> {
            // Should be null in new thread
            assertNull(CaseContextThreadLocal.get(), "Should be null in new thread");

            CaseContextThreadLocal.set(case2Context);
            thread2Result[0] = CaseContextThreadLocal.get().getId();
        });

        thread2.start();
        thread2.join();

        // Main thread context should be unchanged
        assertEquals(case1Id, CaseContextThreadLocal.get().getId(),
                "Main thread context should be unchanged");

        // Thread 2 should have had case2
        assertEquals(case2Id, thread2Result[0], "Thread 2 should have case2 context");
    }

    @Test
    void testMultipleContextSwitches() {
        UUID case1Id = UUID.randomUUID();
        UUID case2Id = UUID.randomUUID();

        CaseContext case1Context = createTestContext(case1Id);
        CaseContext case2Context = createTestContext(case2Id);

        // Switch between contexts
        CaseContextThreadLocal.set(case1Context);
        assertEquals(case1Id, CaseContextThreadLocal.get().getId());

        CaseContextThreadLocal.set(case2Context);
        assertEquals(case2Id, CaseContextThreadLocal.get().getId());

        CaseContextThreadLocal.set(case1Context);
        assertEquals(case1Id, CaseContextThreadLocal.get().getId());
    }
}
