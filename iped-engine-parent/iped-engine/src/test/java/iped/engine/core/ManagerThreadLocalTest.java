package iped.engine.core;

import static org.junit.jupiter.api.Assertions.*;

import iped.engine.config.ConfigurationView;
import iped.engine.data.CaseData;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

/**
 * Tests to verify Manager.getInstance() correctly delegates to ThreadLocal CaseContext.
 *
 * <p>These tests focus on the ThreadLocal delegation pattern rather than Manager initialization,
 * since full Manager creation requires ConfigurationManager setup.
 */
public class ManagerThreadLocalTest {

  @AfterEach
  void tearDown() {
    CaseContextThreadLocal.clear();
  }

  @Test
  void testGetInstanceReturnsNullWithoutContext() {
    assertNull(Manager.getInstance(), "getInstance should return null when no context is set");
  }

  @Test
  void testThreadLocalIsolationBetweenThreads() throws InterruptedException {
    UUID case1Id = UUID.randomUUID();
    UUID case2Id = UUID.randomUUID();

    CaseContext context1 =
        new CaseContext.Builder(case1Id)
            .withManager(null)
            .withCaseData(new CaseData())
            .withConfigurationView(new ConfigurationView())
            .build();

    CaseContext context2 =
        new CaseContext.Builder(case2Id)
            .withManager(null)
            .withCaseData(new CaseData())
            .withConfigurationView(new ConfigurationView())
            .build();

    Boolean[] thread1Cleared = {false};
    Boolean[] thread2Cleared = {false};

    Thread t1 =
        new Thread(
            () -> {
              CaseContextThreadLocal.set(context1);
              assertNotNull(CaseContextThreadLocal.get(), "Thread 1 should have context");
              assertEquals(case1Id, CaseContextThreadLocal.get().getId());
              CaseContextThreadLocal.clear();
              thread1Cleared[0] = CaseContextThreadLocal.get() == null;
            });

    Thread t2 =
        new Thread(
            () -> {
              CaseContextThreadLocal.set(context2);
              assertNotNull(CaseContextThreadLocal.get(), "Thread 2 should have context");
              assertEquals(case2Id, CaseContextThreadLocal.get().getId());
              CaseContextThreadLocal.clear();
              thread2Cleared[0] = CaseContextThreadLocal.get() == null;
            });

    t1.start();
    t2.start();
    t1.join();
    t2.join();

    assertTrue(thread1Cleared[0], "Thread 1 should have cleared context");
    assertTrue(thread2Cleared[0], "Thread 2 should have cleared context");
    assertNull(CaseContextThreadLocal.get(), "Main thread should still have no context");
  }

  @Test
  void testContextNotLeakBetweenThreads() throws InterruptedException {
    UUID caseId = UUID.randomUUID();

    CaseContext context =
        new CaseContext.Builder(caseId)
            .withManager(null)
            .withCaseData(new CaseData())
            .withConfigurationView(new ConfigurationView())
            .build();

    Thread workerThread =
        new Thread(
            () -> {
              CaseContextThreadLocal.set(context);
              assertNotNull(CaseContextThreadLocal.get(), "Worker should have context");
              CaseContextThreadLocal.clear();
            });

    workerThread.start();
    workerThread.join();

    // Context should not leak to main thread
    assertNull(CaseContextThreadLocal.get(), "Context should not leak to main thread");
  }
}
