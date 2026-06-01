package iped.engine.core;

import static org.junit.jupiter.api.Assertions.*;

import java.io.File;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import iped.data.ICaseData;
import iped.engine.config.ConfigurationView;
import iped.engine.data.CaseData;

/**
 * Tests to verify Statistics correctly delegates to ThreadLocal CaseContext.
 *
 * These tests verify that Statistics.get() returns the case-correct instance
 * when called from different threads, enabling per-case statistics isolation.
 */
public class StatisticsThreadLocalTest {

    @AfterEach
    void tearDown() {
        CaseContextThreadLocal.clear();
    }

    @Test
    void testGetReturnsNullWithoutContext() {
        assertNull(Statistics.get(), "Statistics.get() should return null when no context is set");
    }

    @Test
    void testStatisticsIndependentBetweenThreads() throws InterruptedException {
        UUID case1Id = UUID.randomUUID();
        UUID case2Id = UUID.randomUUID();

        ICaseData caseData1 = new CaseData();
        ICaseData caseData2 = new CaseData();
        File indexDir1 = new File("./test-index-1");
        File indexDir2 = new File("./test-index-2");

        Statistics stats1 = new Statistics(caseData1, indexDir1);
        Statistics stats2 = new Statistics(caseData2, indexDir2);

        CaseContext context1 = new CaseContext.Builder(case1Id)
                .withStatistics(stats1)
                .withCaseData(caseData1)
                .withConfigurationView(new ConfigurationView())
                .build();

        CaseContext context2 = new CaseContext.Builder(case2Id)
                .withStatistics(stats2)
                .withCaseData(caseData2)
                .withConfigurationView(new ConfigurationView())
                .build();

        // Thread 1 processes items in context1
        int[] thread1Count = { 0 };
        Thread t1 = new Thread(() -> {
            CaseContextThreadLocal.set(context1);
            Statistics.get().incProcessed();
            Statistics.get().incProcessed();
            thread1Count[0] = Statistics.get().getProcessed();
            CaseContextThreadLocal.clear();
        });

        // Thread 2 processes items in context2
        int[] thread2Count = { 0 };
        Thread t2 = new Thread(() -> {
            CaseContextThreadLocal.set(context2);
            Statistics.get().incProcessed();
            Statistics.get().incProcessed();
            Statistics.get().incProcessed();
            thread2Count[0] = Statistics.get().getProcessed();
            CaseContextThreadLocal.clear();
        });

        t1.start();
        t2.start();
        t1.join();
        t2.join();

        assertEquals(2, thread1Count[0], "Thread 1 should have processed 2 items");
        assertEquals(3, thread2Count[0], "Thread 2 should have processed 3 items");
    }

    @Test
    void testStatisticsVolumeTrackingPerCase() throws InterruptedException {
        UUID case1Id = UUID.randomUUID();
        UUID case2Id = UUID.randomUUID();

        ICaseData caseData1 = new CaseData();
        ICaseData caseData2 = new CaseData();
        File indexDir1 = new File("./test-index-1");
        File indexDir2 = new File("./test-index-2");

        Statistics stats1 = new Statistics(caseData1, indexDir1);
        Statistics stats2 = new Statistics(caseData2, indexDir2);

        CaseContext context1 = new CaseContext.Builder(case1Id)
                .withStatistics(stats1)
                .withCaseData(caseData1)
                .withConfigurationView(new ConfigurationView())
                .build();

        CaseContext context2 = new CaseContext.Builder(case2Id)
                .withStatistics(stats2)
                .withCaseData(caseData2)
                .withConfigurationView(new ConfigurationView())
                .build();

        long[] thread1Volume = { 0 };
        Thread t1 = new Thread(() -> {
            CaseContextThreadLocal.set(context1);
            Statistics.get().addVolume(1000000);
            Statistics.get().addVolume(500000);
            thread1Volume[0] = Statistics.get().getVolume();
            CaseContextThreadLocal.clear();
        });

        long[] thread2Volume = { 0 };
        Thread t2 = new Thread(() -> {
            CaseContextThreadLocal.set(context2);
            Statistics.get().addVolume(2000000);
            thread2Volume[0] = Statistics.get().getVolume();
            CaseContextThreadLocal.clear();
        });

        t1.start();
        t2.start();
        t1.join();
        t2.join();

        assertEquals(1500000L, thread1Volume[0], "Case 1 should have 1.5MB volume");
        assertEquals(2000000L, thread2Volume[0], "Case 2 should have 2MB volume");
    }

    @Test
    void testStatisticsErrorCountingPerCase() throws InterruptedException {
        UUID case1Id = UUID.randomUUID();
        UUID case2Id = UUID.randomUUID();

        ICaseData caseData1 = new CaseData();
        ICaseData caseData2 = new CaseData();
        File indexDir1 = new File("./test-index-1");
        File indexDir2 = new File("./test-index-2");

        Statistics stats1 = new Statistics(caseData1, indexDir1);
        Statistics stats2 = new Statistics(caseData2, indexDir2);

        CaseContext context1 = new CaseContext.Builder(case1Id)
                .withStatistics(stats1)
                .withCaseData(caseData1)
                .withConfigurationView(new ConfigurationView())
                .build();

        CaseContext context2 = new CaseContext.Builder(case2Id)
                .withStatistics(stats2)
                .withCaseData(caseData2)
                .withConfigurationView(new ConfigurationView())
                .build();

        int[] thread1Errors = { 0 };
        Thread t1 = new Thread(() -> {
            CaseContextThreadLocal.set(context1);
            Statistics.get().incTimeouts();
            Statistics.get().incTimeouts();
            thread1Errors[0] = Statistics.get().getTimeouts();
            CaseContextThreadLocal.clear();
        });

        int[] thread2Errors = { 0 };
        Thread t2 = new Thread(() -> {
            CaseContextThreadLocal.set(context2);
            Statistics.get().incTimeouts();
            Statistics.get().incTimeouts();
            Statistics.get().incTimeouts();
            Statistics.get().incTimeouts();
            thread2Errors[0] = Statistics.get().getTimeouts();
            CaseContextThreadLocal.clear();
        });

        t1.start();
        t2.start();
        t1.join();
        t2.join();

        assertEquals(2, thread1Errors[0], "Case 1 should have 2 timeouts");
        assertEquals(4, thread2Errors[0], "Case 2 should have 4 timeouts");
    }
}
