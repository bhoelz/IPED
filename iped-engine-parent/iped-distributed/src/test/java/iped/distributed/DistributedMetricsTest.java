package iped.distributed;

import iped.distributed.coordinator.AgentAvailability;
import iped.distributed.coordinator.AgentRegistration;
import iped.distributed.coordinator.AgentRegistry;
import iped.distributed.metrics.DistributedMetrics;
import iped.distributed.status.ItemStatusEvent;
import iped.distributed.kafka.KafkaItemMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for {@link DistributedMetrics}: event recording, Prometheus text format,
 * label escaping, and thread safety.
 *
 * <p>No Kafka broker or coordinator is required.
 */
class DistributedMetricsTest {

    private DistributedMetrics metrics;
    private AgentRegistry      registry;

    @BeforeEach
    void setup() {
        metrics  = new DistributedMetrics();
        registry = new AgentRegistry(60);
    }

    // =========================================================================
    // Event recording — basic cases
    // =========================================================================

    @Test
    void completedEventIncrementsProcessedCounter() {
        metrics.recordEvent(completedEvent("case1", "HashTask", 1, 100));
        String scrape = metrics.scrape(registry, Map.of());
        assertTrue(scrape.contains("iped_distributed_items_processed_total"),
                "processed metric must appear after a COMPLETED event");
        assertTrue(scrape.contains("{case_id=\"case1\",task_type=\"HashTask\",stage=\"1\"} 1"),
                "counter value must be 1");
    }

    @Test
    void multipleCompletedEventsAccumulate() {
        for (int i = 0; i < 5; i++) {
            metrics.recordEvent(completedEvent("case1", "HashTask", 1, 50));
        }
        String scrape = metrics.scrape(registry, Map.of());
        assertTrue(scrape.contains("} 5"), "counter must sum to 5");
    }

    @Test
    void errorEventIncrementsFailedCounter() {
        metrics.recordEvent(errorEvent("case1", "HashTask", 1));
        String scrape = metrics.scrape(registry, Map.of());
        assertTrue(scrape.contains("iped_distributed_items_failed_total"));
        assertTrue(scrape.contains("{case_id=\"case1\",task_type=\"HashTask\",stage=\"1\"} 1"));
    }

    @Test
    void durationTotalAccumulates() {
        metrics.recordEvent(completedEvent("case1", "HashTask", 1, 200));
        metrics.recordEvent(completedEvent("case1", "HashTask", 1, 300));
        String scrape = metrics.scrape(registry, Map.of());
        assertTrue(scrape.contains("iped_distributed_processing_duration_ms_total"));
        assertTrue(scrape.contains("} 500"), "duration must sum to 200+300=500ms");
    }

    @Test
    void processedAndFailedAreIndependentCounters() {
        metrics.recordEvent(completedEvent("case1", "HashTask", 1, 100));
        metrics.recordEvent(completedEvent("case1", "HashTask", 1, 100));
        metrics.recordEvent(errorEvent("case1", "HashTask", 1));

        Map<String, Long> counters = metrics.countersForCase("case1");
        assertEquals(2L, counters.get("processed[HashTask][stage=1]"));
        assertEquals(1L, counters.get("failed[HashTask][stage=1]"));
    }

    // =========================================================================
    // Events without a task type are ignored
    // =========================================================================

    @Test
    void discoveredEventIgnored() {
        ItemStatusEvent e = new ItemStatusEvent();
        e.setType(ItemStatusEvent.Type.DISCOVERED);
        e.setCaseId("case1");
        // no taskType
        metrics.recordEvent(e);
        assertEquals("", metrics.scrape(registry, Map.of()),
                "DISCOVERED events have no taskType and must not create metric entries");
    }

    @Test
    void caseCompletedEventIgnored() {
        metrics.recordEvent(ItemStatusEvent.caseCompleted("case1"));
        assertEquals("", metrics.scrape(registry, Map.of()));
    }

    @Test
    void nullEventIgnored() {
        assertDoesNotThrow(() -> metrics.recordEvent(null));
        assertEquals("", metrics.scrape(registry, Map.of()));
    }

    @Test
    void eventWithNullTaskTypeIgnored() {
        ItemStatusEvent e = new ItemStatusEvent();
        e.setType(ItemStatusEvent.Type.COMPLETED);
        e.setCaseId("case1");
        e.setTaskType(null);
        metrics.recordEvent(e);
        assertEquals("", metrics.scrape(registry, Map.of()));
    }

    // =========================================================================
    // Multiple cases and stages are tracked independently
    // =========================================================================

    @Test
    void multiCaseCountersAreIsolated() {
        metrics.recordEvent(completedEvent("caseA", "HashTask", 1, 10));
        metrics.recordEvent(completedEvent("caseA", "HashTask", 1, 10));
        metrics.recordEvent(completedEvent("caseB", "HashTask", 1, 10));

        Map<String, Long> caseA = metrics.countersForCase("caseA");
        Map<String, Long> caseB = metrics.countersForCase("caseB");

        assertEquals(2L, caseA.get("processed[HashTask][stage=1]"));
        assertEquals(1L, caseB.get("processed[HashTask][stage=1]"));
    }

    @Test
    void multiStageCountersAreIsolated() {
        metrics.recordEvent(completedEvent("c1", "HashTask",  1, 10));
        metrics.recordEvent(completedEvent("c1", "IndexTask", 2, 20));

        Map<String, Long> counters = metrics.countersForCase("c1");
        assertEquals(1L, counters.get("processed[HashTask][stage=1]"));
        assertEquals(1L, counters.get("processed[IndexTask][stage=2]"));
    }

    @Test
    void scrapeContainsAllCasesAndStages() {
        metrics.recordEvent(completedEvent("case1", "HashTask",  1, 10));
        metrics.recordEvent(completedEvent("case2", "IndexTask", 3, 20));

        String scrape = metrics.scrape(registry, Map.of());
        assertTrue(scrape.contains("case_id=\"case1\""));
        assertTrue(scrape.contains("case_id=\"case2\""));
        assertTrue(scrape.contains("task_type=\"HashTask\""));
        assertTrue(scrape.contains("task_type=\"IndexTask\""));
    }

    // =========================================================================
    // Prometheus text format structure
    // =========================================================================

    @Test
    void scrapeHasHelpAndTypeLines() {
        metrics.recordEvent(completedEvent("c1", "T", 0, 1));
        String scrape = metrics.scrape(registry, Map.of());
        assertTrue(scrape.contains("# HELP iped_distributed_items_processed_total"),
                "HELP line required");
        assertTrue(scrape.contains("# TYPE iped_distributed_items_processed_total counter"),
                "TYPE line required");
    }

    @Test
    void scrapeMetricNamesAreLowercaseUnderscored() {
        metrics.recordEvent(completedEvent("c1", "T", 0, 1));
        String scrape = metrics.scrape(registry, Map.of());
        // No uppercase in metric names (Prometheus convention)
        for (String line : scrape.split("\n")) {
            if (line.startsWith("#")) continue;
            String name = line.contains("{") ? line.substring(0, line.indexOf('{')) : line.split(" ")[0];
            assertEquals(name.toLowerCase(), name,
                    "metric name must be lowercase: " + name);
        }
    }

    @Test
    void scrapeEndsWithNewline() {
        metrics.recordEvent(completedEvent("c1", "T", 0, 1));
        String scrape = metrics.scrape(registry, Map.of());
        assertTrue(scrape.endsWith("\n"), "Prometheus text must end with a newline");
    }

    @Test
    void emptyScrapeWhenNoEvents() {
        String scrape = metrics.scrape(registry, Map.of());
        assertEquals("", scrape, "no HELP/TYPE lines when nothing has been recorded");
    }

    // =========================================================================
    // Agent gauges
    // =========================================================================

    @Test
    void agentGaugesAppearsInScrape() {
        AgentRegistration a = AgentRegistration.of("agent-1", "HashTask", 1, 8, "host-a");
        a.setCurrentLoad(3);
        a.setFreeSlots(5);
        registry.register(a);

        String scrape = metrics.scrape(registry, Map.of());
        assertTrue(scrape.contains("iped_distributed_agent_inflight"));
        assertTrue(scrape.contains("iped_distributed_agent_free_slots"));
        assertTrue(scrape.contains("agent_id=\"agent-1\""));
        assertTrue(scrape.contains("hostname=\"host-a\""));
        assertTrue(scrape.contains("} 3"), "inflight must be 3");
        assertTrue(scrape.contains("} 5"), "free slots must be 5");
    }

    @Test
    void agentGaugesAbsentWhenNoAgentsRegistered() {
        String scrape = metrics.scrape(registry, Map.of());
        assertFalse(scrape.contains("iped_distributed_agent_inflight"),
                "gauge family must not appear when no agents are registered");
    }

    @Test
    void multipleAgentsAllAppear() {
        registry.register(AgentRegistration.of("a1", "HashTask",  1, 4, "h1"));
        registry.register(AgentRegistration.of("a2", "IndexTask", 2, 4, "h2"));
        String scrape = metrics.scrape(registry, Map.of());
        assertTrue(scrape.contains("agent_id=\"a1\""));
        assertTrue(scrape.contains("agent_id=\"a2\""));
    }

    // =========================================================================
    // Consumer lag
    // =========================================================================

    @Test
    void lagFamilyAppearsWhenNonEmpty() {
        DistributedMetrics.LagKey key = new DistributedMetrics.LagKey(
                "case1", "case1.HashTask", "iped.case1.stage.1", 0);
        String scrape = metrics.scrape(registry, Map.of(key, 7L));
        assertTrue(scrape.contains("iped_distributed_consumer_lag"));
        assertTrue(scrape.contains("} 7"), "lag value must be 7");
        assertTrue(scrape.contains("consumer_group=\"case1.HashTask\""));
        assertTrue(scrape.contains("partition=\"0\""));
    }

    @Test
    void lagFamilyAbsentWhenEmpty() {
        String scrape = metrics.scrape(registry, Map.of());
        assertFalse(scrape.contains("iped_distributed_consumer_lag"),
                "lag family must be omitted when lagByKey is empty");
    }

    @Test
    void multipleLagPartitions() {
        Map<DistributedMetrics.LagKey, Long> lag = Map.of(
                new DistributedMetrics.LagKey("c", "c.T", "iped.c.stage.1", 0), 3L,
                new DistributedMetrics.LagKey("c", "c.T", "iped.c.stage.1", 1), 0L,
                new DistributedMetrics.LagKey("c", "c.T", "iped.c.stage.1", 2), 12L
        );
        String scrape = metrics.scrape(registry, lag);
        long sampleCount = scrape.lines()
                .filter(l -> l.startsWith("iped_distributed_consumer_lag{"))
                .count();
        assertEquals(3L, sampleCount, "one sample per partition");
    }

    // =========================================================================
    // Label escaping
    // =========================================================================

    @Test
    void escapeHandlesNull() {
        assertEquals("", DistributedMetrics.escape(null));
    }

    @Test
    void escapeHandlesPlainString() {
        assertEquals("HashTask", DistributedMetrics.escape("HashTask"));
    }

    @Test
    void escapeQuotesAreEscaped() {
        assertEquals("a\\\"b", DistributedMetrics.escape("a\"b"),
                "double-quote must be escaped as \\\"");
    }

    @Test
    void escapeBackslashesAreEscaped() {
        assertEquals("a\\\\b", DistributedMetrics.escape("a\\b"),
                "backslash must be escaped as \\\\");
    }

    @Test
    void escapeNewlinesAreEscaped() {
        assertEquals("a\\nb", DistributedMetrics.escape("a\nb"),
                "newline must be escaped as \\n");
    }

    @Test
    void escapedLabelValueDoesNotBreakPrometheusFormat() {
        // A case ID containing a quote — the scrape output must remain parseable
        metrics.recordEvent(completedEvent("case\"weird", "Hash\"Task", 1, 10));
        String scrape = metrics.scrape(registry, Map.of());
        // Verify the line is well-formed: value follows after "} "
        assertTrue(scrape.lines()
                .filter(l -> l.startsWith("iped_distributed_items_processed_total{"))
                .anyMatch(l -> l.endsWith("} 1")),
                "scrape line must end with the sample value despite escaped label");
    }

    // =========================================================================
    // Thread safety
    // =========================================================================

    @Test
    void concurrentRecordEventsAreAllCounted() throws InterruptedException {
        int threads = 8;
        int eventsPerThread = 500;
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        CountDownLatch start = new CountDownLatch(1);
        CountDownLatch done  = new CountDownLatch(threads);

        for (int t = 0; t < threads; t++) {
            pool.submit(() -> {
                try {
                    start.await();
                    for (int i = 0; i < eventsPerThread; i++) {
                        metrics.recordEvent(completedEvent("c1", "HashTask", 1, 10));
                    }
                } catch (InterruptedException ignored) {
                } finally {
                    done.countDown();
                }
            });
        }

        start.countDown();
        done.await();
        pool.shutdown();

        Map<String, Long> counters = metrics.countersForCase("c1");
        assertEquals((long) threads * eventsPerThread,
                counters.get("processed[HashTask][stage=1]"),
                "all concurrent increments must be counted without loss");
    }

    // =========================================================================
    // countersForCase — operator query
    // =========================================================================

    @Test
    void countersForCaseOnlyReturnsRequestedCase() {
        metrics.recordEvent(completedEvent("caseA", "T", 1, 1));
        metrics.recordEvent(completedEvent("caseB", "T", 1, 1));

        Map<String, Long> caseA = metrics.countersForCase("caseA");
        assertFalse(caseA.isEmpty());
        caseA.forEach((k, v) -> assertFalse(k.contains("caseB"),
                "countersForCase must not leak entries from other cases"));
    }

    @Test
    void countersForCaseEmptyWhenNoEventsRecorded() {
        assertTrue(metrics.countersForCase("nonexistent").isEmpty());
    }

    // =========================================================================
    // Helpers
    // =========================================================================

    private static ItemStatusEvent completedEvent(String caseId, String taskType,
                                                   int stage, long durationMs) {
        KafkaItemMessage msg = new KafkaItemMessage();
        msg.setCaseId(caseId);
        msg.setItemUuid(java.util.UUID.randomUUID().toString());
        msg.setPipelineStage(stage);
        ItemStatusEvent e = ItemStatusEvent.completed(msg, taskType, durationMs);
        return e;
    }

    private static ItemStatusEvent errorEvent(String caseId, String taskType, int stage) {
        KafkaItemMessage msg = new KafkaItemMessage();
        msg.setCaseId(caseId);
        msg.setItemUuid(java.util.UUID.randomUUID().toString());
        msg.setPipelineStage(stage);
        return ItemStatusEvent.error(msg, taskType, 0, new RuntimeException("test error"));
    }
}
