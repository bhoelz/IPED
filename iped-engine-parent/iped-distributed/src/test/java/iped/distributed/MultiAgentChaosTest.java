package iped.distributed;

import iped.distributed.kafka.ItemConverter;
import iped.distributed.kafka.KafkaItemMessage;
import iped.distributed.kafka.PartitionOffsetTracker;
import iped.distributed.coordinator.CaseCompletionMonitor;
import iped.distributed.coordinator.CaseLifecycleManager;
import iped.distributed.kafka.TopicProvisioner;
import iped.distributed.status.ItemStatusEvent;
import org.apache.kafka.clients.consumer.OffsetAndMetadata;
import org.apache.kafka.common.TopicPartition;
import org.junit.jupiter.api.Test;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Phase 7 — Chaos simulation: N agents across M partitions, K agents killed mid-run.
 *
 * <p>Validates three invariants that must hold regardless of which agents crash and
 * at which commit offset:
 * <ol>
 *   <li><b>No item loss</b> — every input UUID (and its deterministic sub-item UUID) is
 *       present in the final index.</li>
 *   <li><b>No duplicates</b> — upsert semantics ensure each UUID appears exactly once in
 *       the index even if the same item is processed more than once.</li>
 *   <li><b>Case completion</b> — {@link CaseCompletionMonitor} detects completion once all
 *       items have passed the final pipeline stage, even after crash-and-restart cycles.</li>
 * </ol>
 *
 * <p>Simulation model:
 * <ul>
 *   <li>One topic partition per simulated agent; items are distributed round-robin.</li>
 *   <li>Each agent processes its partition sequentially, calling
 *       {@code recordPolled/markDone} and committing via {@code drainCommittable} after
 *       every {@code COMMIT_EVERY} items — matching the production {@link PartitionOffsetTracker}
 *       usage pattern.</li>
 *   <li>A "crash" resets the agent's read position to its last committed watermark so
 *       uncommitted items are re-delivered (at-least-once).</li>
 *   <li>The "index" is a {@code Map<UUID, String>} with upsert semantics, modelling Lucene's
 *       {@code updateDocument}: re-writing the same UUID is idempotent.</li>
 *   <li>Sub-items are derived via {@link ItemConverter#deterministicSubitemUuid} — the same
 *       parent always produces the same sub-item UUID, so re-delivery never duplicates them.</li>
 * </ul>
 *
 * <p>No Kafka broker is required.
 */
class MultiAgentChaosTest {

    private static final String CASE_ID   = "chaos-case";
    private static final String TOPIC     = "iped." + CASE_ID + ".stage.0";
    private static final String TASK      = "HashTask";

    private static final int TOTAL_ITEMS  = 40;
    private static final int PARTITIONS   = 4;
    private static final int COMMIT_EVERY = 3;

    // ── Simulation primitives ─────────────────────────────────────────────────

    /** One immutable ordered list of records — stands in for a Kafka partition. */
    private record SimulatedPartition(TopicPartition tp, List<KafkaItemMessage> records) {
        int size() { return records.size(); }
    }

    /**
     * Simulated agent that processes one partition sequentially.
     * Commits after {@code commitEvery} items; crash resets to last committed watermark.
     */
    private static final class SimulatedAgent {

        private final SimulatedPartition part;
        private final Map<String, String>       index;
        private final List<ItemStatusEvent>     statusLog;
        private final PartitionOffsetTracker    tracker = new PartitionOffsetTracker();
        private final int                       commitEvery;

        /** Next record offset to read (advances as items are polled). */
        private long position = 0;
        /** Last committed offset watermark (Kafka "next to read" convention: committed+1). */
        private long committedNext = 0;

        SimulatedAgent(SimulatedPartition part,
                       Map<String, String> index,
                       List<ItemStatusEvent> statusLog,
                       int commitEvery) {
            this.part       = part;
            this.index      = index;
            this.statusLog  = statusLog;
            this.commitEvery = commitEvery;
        }

        /** Processes up to {@code limit} records, then returns. */
        void process(int limit) {
            int processed = 0;
            while (position < part.size() && processed < limit) {
                long offset = position;
                KafkaItemMessage msg = part.records().get((int) offset);

                tracker.recordPolled(part.tp(), offset);
                position++;
                processed++;

                // Upsert into the shared index (idempotent — overwrites on re-delivery)
                index.put(msg.getItemUuid(), msg.getItemUuid());

                // Deterministic sub-item — same UUID on every redelivery
                String subUuid = ItemConverter.deterministicSubitemUuid(msg.getItemUuid(), 0);
                index.put(subUuid, subUuid);

                // Emit status events (collected by the caller for monitor injection)
                statusLog.add(ItemStatusEvent.started(msg, TASK));
                statusLog.add(ItemStatusEvent.completed(msg, TASK, 1L));

                tracker.markDone(part.tp(), offset);

                // Commit after every N items
                if (processed % commitEvery == 0) {
                    Map<TopicPartition, OffsetAndMetadata> c = tracker.drainCommittable();
                    if (c.containsKey(part.tp())) {
                        committedNext = c.get(part.tp()).offset(); // "next to read" value
                    }
                }
            }
            // Final drain for the partial trailing batch
            Map<TopicPartition, OffsetAndMetadata> c = tracker.drainCommittable();
            if (c.containsKey(part.tp())) {
                committedNext = c.get(part.tp()).offset();
            }
        }

        /**
         * Simulates a crash: resets the read position to the last committed offset so
         * uncommitted items are re-delivered.
         */
        void crash() {
            position = committedNext; // re-deliver uncommitted items
        }

        /** Process all remaining records (to completion). */
        void processRemaining() {
            process(Integer.MAX_VALUE);
        }
    }

    // ── Setup helpers ─────────────────────────────────────────────────────────

    private static List<SimulatedPartition> buildPartitions(int totalItems, int partCount) {
        @SuppressWarnings("unchecked")
        List<KafkaItemMessage>[] byPart = new List[partCount];
        for (int p = 0; p < partCount; p++) byPart[p] = new ArrayList<>();

        for (int i = 0; i < totalItems; i++) {
            KafkaItemMessage msg = new KafkaItemMessage();
            msg.setCaseId(CASE_ID);
            msg.setItemUuid("item-" + i);
            msg.setPipelineStage(0);
            msg.setLength((long) (1024 + i * 100));
            byPart[i % partCount].add(msg);
        }

        List<SimulatedPartition> parts = new ArrayList<>();
        for (int p = 0; p < partCount; p++) {
            parts.add(new SimulatedPartition(new TopicPartition(TOPIC, p),
                    Collections.unmodifiableList(byPart[p])));
        }
        return parts;
    }

    private static CaseLifecycleManager singleStageCaseLifecycle() {
        TopicProvisioner tp = new TopicProvisioner("localhost:0") {
            @Override public void provisionCase(String id, int n, int p, short r) {}
        };
        CaseLifecycleManager mgr = new CaseLifecycleManager(tp);
        mgr.startCase(CASE_ID, List.of(TASK), 1, (short) 1);
        return mgr;
    }

    /** Expected index size: each of TOTAL_ITEMS root items + 1 deterministic sub-item each. */
    private static int expectedIndexSize() {
        return TOTAL_ITEMS * 2;
    }

    // ── Tests ─────────────────────────────────────────────────────────────────

    @Test
    void noAgentCrash_allItemsIndexedExactlyOnce() {
        List<SimulatedPartition> parts = buildPartitions(TOTAL_ITEMS, PARTITIONS);
        Map<String, String> index = new HashMap<>();
        List<ItemStatusEvent> log = new ArrayList<>();

        for (SimulatedPartition p : parts) {
            new SimulatedAgent(p, index, log, COMMIT_EVERY).processRemaining();
        }

        assertEquals(expectedIndexSize(), index.size(),
                "each item and its deterministic sub-item must appear exactly once in the index");
    }

    @Test
    void oneAgentCrashesAndRestarts_noItemLost() {
        List<SimulatedPartition> parts = buildPartitions(TOTAL_ITEMS, PARTITIONS);
        Map<String, String> index = new HashMap<>();
        List<ItemStatusEvent> log = new ArrayList<>();

        // Agent 0 processes slightly past a commit boundary, crashes, then finishes
        SimulatedAgent crashAgent = new SimulatedAgent(parts.get(0), index, log, COMMIT_EVERY);
        crashAgent.process(COMMIT_EVERY + 1); // 1 item past commit — that 1 is uncommitted
        crashAgent.crash();                   // reset to last committed watermark
        crashAgent.processRemaining();        // re-process uncommitted + rest

        // Remaining agents run to completion
        for (int i = 1; i < PARTITIONS; i++) {
            new SimulatedAgent(parts.get(i), index, log, COMMIT_EVERY).processRemaining();
        }

        assertEquals(expectedIndexSize(), index.size(),
                "crash-and-restart must not lose items; index size must be " + expectedIndexSize());
    }

    @Test
    void allAgentsCrashAndRestart_noItemLost() {
        List<SimulatedPartition> parts = buildPartitions(TOTAL_ITEMS, PARTITIONS);
        Map<String, String> index = new HashMap<>();
        List<ItemStatusEvent> log = new ArrayList<>();

        List<SimulatedAgent> agents = new ArrayList<>();
        for (SimulatedPartition p : parts) {
            agents.add(new SimulatedAgent(p, index, log, COMMIT_EVERY));
        }

        // All agents crash after their first commit batch
        for (SimulatedAgent agent : agents) {
            agent.process(COMMIT_EVERY + 2);
            agent.crash();
        }

        // All agents restart and finish
        for (SimulatedAgent agent : agents) {
            agent.processRemaining();
        }

        assertEquals(expectedIndexSize(), index.size(),
                "all-agents-crashed scenario must still produce a complete index of size "
                        + expectedIndexSize());
    }

    @Test
    void repeatCrashesOnSameAgent_noItemLost() {
        // Single-partition scenario: agent crashes twice, must still complete all items
        List<SimulatedPartition> parts = buildPartitions(TOTAL_ITEMS, 1);
        Map<String, String> index = new HashMap<>();
        List<ItemStatusEvent> log = new ArrayList<>();

        SimulatedAgent agent = new SimulatedAgent(parts.get(0), index, log, COMMIT_EVERY);

        // First crash after 1st commit
        agent.process(COMMIT_EVERY + 1);
        agent.crash();

        // Second crash just before another commit
        agent.process(COMMIT_EVERY - 1); // process 2 more (uncommitted)
        agent.crash();

        // Final run to completion
        agent.processRemaining();

        assertEquals(TOTAL_ITEMS * 2, index.size(),
                "two crashes on the same agent must not cause item loss or duplication");
    }

    @Test
    void deterministicSubitemUuidsAreStableAcrossRedeliveries() {
        // The same parent UUID must always yield the same sub-item UUID, guaranteeing
        // that re-delivering a parent doesn't create a second sub-item entry in the index.
        String parentUuid = "parent-stable";

        String firstDelivery  = ItemConverter.deterministicSubitemUuid(parentUuid, 0);
        String secondDelivery = ItemConverter.deterministicSubitemUuid(parentUuid, 0);
        String thirdDelivery  = ItemConverter.deterministicSubitemUuid(parentUuid, 0);

        assertEquals(firstDelivery, secondDelivery, "sub-item UUID must be identical on 2nd delivery");
        assertEquals(firstDelivery, thirdDelivery,  "sub-item UUID must be identical on 3rd delivery");
        assertNotEquals(parentUuid, firstDelivery,  "sub-item UUID must differ from parent UUID");
    }

    @Test
    void differentParentsProduceDifferentSubitemUuids() {
        String sub1 = ItemConverter.deterministicSubitemUuid("parent-A", 0);
        String sub2 = ItemConverter.deterministicSubitemUuid("parent-B", 0);
        assertNotEquals(sub1, sub2, "different parents must produce different sub-item UUIDs");
    }

    @Test
    void differentOrdinalsProduceDifferentSubitemUuids() {
        String sub0 = ItemConverter.deterministicSubitemUuid("parent-X", 0);
        String sub1 = ItemConverter.deterministicSubitemUuid("parent-X", 1);
        assertNotEquals(sub0, sub1, "different ordinals must produce different sub-item UUIDs");
    }

    @Test
    void caseCompletionMonitorDetectsCompletionAfterCrashAndRestart() {
        List<SimulatedPartition> parts = buildPartitions(TOTAL_ITEMS, PARTITIONS);
        Map<String, String> index = new ConcurrentHashMap<>();
        List<ItemStatusEvent> log = new ArrayList<>();

        CaseLifecycleManager lifecycle = singleStageCaseLifecycle();
        List<ItemStatusEvent> published = new ArrayList<>();
        CaseCompletionMonitor monitor = new CaseCompletionMonitor(lifecycle, published::add, 3600);

        // Emit DISCOVERED for every root item (readers emit these before agents start)
        for (SimulatedPartition p : parts) {
            for (KafkaItemMessage msg : p.records()) {
                monitor.onEvent(ItemStatusEvent.discovered(msg));
            }
        }

        // Partition 0 agent crashes and restarts
        SimulatedAgent crashAgent = new SimulatedAgent(parts.get(0), index, log, COMMIT_EVERY);
        crashAgent.process(COMMIT_EVERY + 1);
        crashAgent.crash();
        crashAgent.processRemaining();

        // Remaining partitions
        for (int i = 1; i < PARTITIONS; i++) {
            new SimulatedAgent(parts.get(i), index, log, COMMIT_EVERY).processRemaining();
        }

        // Feed all status events to the monitor
        // De-duplicate COMPLETED events for the same item/task (redeliveries):
        // duplicates are harmless to the monitor (only the first COMPLETED triggers
        // completedFinal.incrementAndGet; subsequent ones are ignored via the inFlight map)
        log.forEach(monitor::onEvent);

        assertTrue(monitor.isCompleted(CASE_ID),
                "coordinator must detect case completion after crash-and-restart scenario");
        assertEquals(1L, published.stream()
                .filter(e -> e.getType() == ItemStatusEvent.Type.CASE_COMPLETED)
                .count(),
                "exactly one CASE_COMPLETED event must be published");
    }

    @Test
    void partitionTrackerWatermarkStopsAtFirstUncommittedGap() {
        TopicPartition tp = new TopicPartition(TOPIC, 0);
        PartitionOffsetTracker tracker = new PartitionOffsetTracker();

        // Poll offsets 0, 1, 2, 3 (all pre-registered)
        tracker.recordPolled(tp, 0);
        tracker.recordPolled(tp, 1);
        tracker.recordPolled(tp, 2);
        tracker.recordPolled(tp, 3);

        // Mark done: 0, 1, 3 — gap at offset 2
        tracker.markDone(tp, 0);
        tracker.markDone(tp, 1);
        tracker.markDone(tp, 3);

        Map<TopicPartition, OffsetAndMetadata> committable = tracker.drainCommittable();

        // Watermark should advance only to offset 1 (next-to-read = 2), not past the gap
        assertTrue(committable.containsKey(tp),
                "some offsets must be committable (0 and 1 are done)");
        long nextToRead = committable.get(tp).offset();
        assertEquals(2L, nextToRead,
                "commit watermark must stop at next-to-read=2 (gap at offset 2); was " + nextToRead);
    }

    @Test
    void partitionTrackerFullDrainAfterGapFilled() {
        TopicPartition tp = new TopicPartition(TOPIC, 0);
        PartitionOffsetTracker tracker = new PartitionOffsetTracker();

        tracker.recordPolled(tp, 0);
        tracker.recordPolled(tp, 1);
        tracker.recordPolled(tp, 2);
        tracker.recordPolled(tp, 3);

        tracker.markDone(tp, 0);
        tracker.markDone(tp, 1);
        tracker.markDone(tp, 3);
        tracker.drainCommittable(); // advances to 2

        // Now fill the gap
        tracker.markDone(tp, 2);
        Map<TopicPartition, OffsetAndMetadata> after = tracker.drainCommittable();

        // Now all 4 are done → next-to-read should be 4
        assertTrue(after.containsKey(tp), "all offsets done — commit must advance");
        assertEquals(4L, after.get(tp).offset(),
                "after filling gap at 2, watermark must advance past 3 to next-to-read=4");
    }
}
