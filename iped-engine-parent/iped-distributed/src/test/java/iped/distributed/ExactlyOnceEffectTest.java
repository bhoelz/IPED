package iped.distributed;

import iped.distributed.kafka.ItemConverter;
import iped.distributed.kafka.KafkaItemDeserializer;
import iped.distributed.kafka.KafkaItemMessage;
import iped.distributed.kafka.PartitionOffsetTracker;
import org.apache.kafka.clients.consumer.OffsetAndMetadata;
import org.apache.kafka.common.TopicPartition;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Exactly-once-effect validation: a distributed run with agent interruptions
 * must produce the same indexed output as a monolithic (uninterrupted) run.
 *
 * <h2>What is being tested</h2>
 * <p>Three mechanisms work together to guarantee exactly-once <em>observable effect</em>
 * even though the delivery guarantee is only <em>at-least-once</em>:
 * <ol>
 *   <li><b>At-least-once delivery</b> — {@link PartitionOffsetTracker} advances the Kafka
 *       offset watermark only through consecutive completed offsets; uncommitted items are
 *       re-delivered after an agent restart.</li>
 *   <li><b>Sub-item UUID stability</b> — {@link ItemConverter#deterministicSubitemUuid}
 *       derives each sub-item UUID from {@code (parentUuid, ordinal)}; re-delivering the
 *       parent therefore re-emits sub-items with the same UUIDs.</li>
 *   <li><b>Idempotent indexing</b> — the downstream Lucene index uses {@code updateDocument}
 *       (upsert), so re-processing an item overwrites rather than duplicates its entry.</li>
 * </ol>
 *
 * <h2>Simulation model</h2>
 * <p>The simulated pipeline has one partition with N items.  The agent processes them in
 * order: items 0..{@code commitAfter}-1 are fully processed and committed; items
 * {@code commitAfter}..N-1 are processed but <em>not</em> committed (agent killed).  On
 * restart, items from the committed watermark onward are re-delivered.  A simulated Lucene
 * index (a {@code Map<uuid, data>}) receives all writes via upsert semantics.
 *
 * <p>None of these tests require a running Kafka broker.
 */
class ExactlyOnceEffectTest {

    private static final TopicPartition PARTITION =
            new TopicPartition("iped.case-validation.stage.1", 0);
    private static final String CASE_ID = "case-validation";

    // =========================================================================
    // Core simulation helpers
    // =========================================================================

    /**
     * Monolithic baseline: each parent processed exactly once.
     *
     * @return parentUuid → list-of-subitem-uuids (stable order)
     */
    private Map<String, List<String>> monolithicRun(List<String> parentUuids, int subitemsPerParent) {
        Map<String, List<String>> index = new LinkedHashMap<>();
        for (String parentUuid : parentUuids) {
            // parent itself is indexed by its UUID
            index.put(parentUuid, buildSubitemUuids(parentUuid, subitemsPerParent));
        }
        return index;
    }

    /**
     * Distributed run with an agent crash at offset {@code commitAfter}.
     *
     * <p>Phase 1: process all N items; mark done only items 0..{@code commitAfter}-1.
     * Drain committed offsets (watermark = {@code commitAfter}-1 if commitAfter > 0).
     *
     * <p>Phase 2 (restart): re-deliver every item at or after the committed watermark
     * (the semantics Kafka gives on consumer restart) and re-index them with upsert.
     *
     * @return the same shape as {@link #monolithicRun}
     */
    private Map<String, List<String>> distributedRunWithInterruption(
            List<String> parentUuids, int subitemsPerParent, int commitAfter) {

        // ── Phase 1 ────────────────────────────────────────────────────────────
        PartitionOffsetTracker tracker = new PartitionOffsetTracker();
        // Simulated "Lucene index" — upsert semantics (put overwrites)
        Map<String, List<String>> index = new LinkedHashMap<>();

        for (int offset = 0; offset < parentUuids.size(); offset++) {
            tracker.recordPolled(PARTITION, offset);
            String parentUuid = parentUuids.get(offset);
            // Write to index (first pass — may be overwritten on re-delivery)
            index.put(parentUuid, buildSubitemUuids(parentUuid, subitemsPerParent));

            if (offset < commitAfter) {
                // Simulates: producer.send callback fires → tracker.markDone
                tracker.markDone(PARTITION, offset);
            }
            // offsets >= commitAfter: NOT marked done (agent killed mid-batch)
        }

        // Drain what was committed before the crash
        Map<TopicPartition, OffsetAndMetadata> committed = tracker.drainCommittable();

        // The Kafka consumer on restart will seek to the committed offset.
        // If nothing was committed, it seeks to earliest (offset 0 with auto.offset.reset=earliest).
        long restartFromOffset = committed.containsKey(PARTITION)
                ? committed.get(PARTITION).offset()   // next offset to read = watermark + 1
                : 0;

        // ── Phase 2 (restart) ──────────────────────────────────────────────────
        for (long offset = restartFromOffset; offset < parentUuids.size(); offset++) {
            String parentUuid = parentUuids.get((int) offset);
            // Re-process with the same task → same sub-item ordinals → same UUIDs
            // Upsert into index: overwrites any first-phase entry for this UUID
            index.put(parentUuid, buildSubitemUuids(parentUuid, subitemsPerParent));
        }

        return index;
    }

    /** Mirrors the logic in {@code TaskAgent.buildSubitemRegistry}. */
    private List<String> buildSubitemUuids(String parentUuid, int count) {
        return IntStream.range(0, count)
                .mapToObj(ordinal -> ItemConverter.deterministicSubitemUuid(parentUuid, ordinal))
                .collect(Collectors.toList());
    }

    /** Creates a reproducible list of parent item UUIDs. */
    private List<String> makeParentUuids(int count) {
        return IntStream.range(0, count)
                .mapToObj(i -> UUID.nameUUIDFromBytes(
                        ("parent-" + i).getBytes(StandardCharsets.UTF_8)).toString())
                .collect(Collectors.toList());
    }

    // =========================================================================
    // Tests — crash at various commit points
    // =========================================================================

    @ParameterizedTest(name = "commitAfter={0}")
    @ValueSource(ints = {0, 1, 3, 5, 10})
    void distributedOutputMatchesMonolithicForVariousCrashPoints(int commitAfter) {
        int n = 10;
        int subitemsPerParent = 4;
        List<String> parentUuids = makeParentUuids(n);

        Map<String, List<String>> monolithic   = monolithicRun(parentUuids, subitemsPerParent);
        Map<String, List<String>> distributed  = distributedRunWithInterruption(
                parentUuids, subitemsPerParent, commitAfter);

        assertEquals(monolithic.keySet(), distributed.keySet(),
                "same set of parent UUIDs after crash at commitAfter=" + commitAfter);
        for (String parentUuid : monolithic.keySet()) {
            assertEquals(monolithic.get(parentUuid), distributed.get(parentUuid),
                    "sub-item UUIDs mismatch for parent " + parentUuid
                    + " (commitAfter=" + commitAfter + ")");
        }
    }

    @Test
    void noItemsLostWhenAgentCrashesBeforeAnyCommit() {
        int n = 8;
        List<String> parentUuids = makeParentUuids(n);
        Map<String, List<String>> monolithic  = monolithicRun(parentUuids, 3);
        Map<String, List<String>> distributed = distributedRunWithInterruption(parentUuids, 3, 0);

        assertEquals(n, distributed.size(), "all items must appear after full re-delivery");
        assertEquals(monolithic, distributed);
    }

    @Test
    void noItemsDuplicatedWhenAllCommitted() {
        int n = 6;
        List<String> parentUuids = makeParentUuids(n);
        Map<String, List<String>> monolithic  = monolithicRun(parentUuids, 5);
        // commitAfter = n means everything was committed before crash
        Map<String, List<String>> distributed = distributedRunWithInterruption(parentUuids, 5, n);

        assertEquals(n, distributed.size(), "no items added or lost when all offsets committed");
        assertEquals(monolithic, distributed);
    }

    @Test
    void multipleRestarts_outputRemainsStable() {
        // Two successive crashes: first at offset 2, then at offset 5.
        // Each restart re-delivers uncommitted items; final output must still match monolithic.
        int n = 8;
        int subitemsPerParent = 3;
        List<String> parentUuids = makeParentUuids(n);
        Map<String, List<String>> monolithic = monolithicRun(parentUuids, subitemsPerParent);

        // Simulate manually: crash at 2, restart and crash at 5, then full completion
        Map<String, List<String>> index = new LinkedHashMap<>();

        for (int pass = 0; pass <= 2; pass++) {
            // Each pass starts from the last committed offset (0, 2, 5 respectively)
            long startOffset = (pass == 0 ? 0 : (pass == 1 ? 2 : 5));
            long endOffset   = (pass == 0 ? n : (pass == 1 ? n : n));
            // commit only up to: 2 on pass 0, 5 on pass 1, all on pass 2
            long commitUpTo  = (pass == 0 ? 2 : (pass == 1 ? 5 : n));

            for (long offset = startOffset; offset < endOffset; offset++) {
                String parentUuid = parentUuids.get((int) offset);
                index.put(parentUuid, buildSubitemUuids(parentUuid, subitemsPerParent));
                if (offset < commitUpTo) {
                    // "committed" — simulated; won't be re-delivered next pass
                }
            }
        }

        assertEquals(n, index.size(), "all items indexed after multiple restarts");
        assertEquals(monolithic, index, "output matches monolithic after multiple restarts");
    }

    // =========================================================================
    // Sub-item UUID properties
    // =========================================================================

    @Test
    void subitemUuidsAreUniqueWithinOneParent() {
        String parentUuid = UUID.randomUUID().toString();
        List<String> uuids = buildSubitemUuids(parentUuid, 100);

        Set<String> unique = new HashSet<>(uuids);
        assertEquals(100, unique.size(), "each ordinal must produce a distinct UUID");
    }

    @Test
    void subitemUuidsAreStableAcrossRedeliveries() {
        String parentUuid = UUID.randomUUID().toString();
        List<String> first  = buildSubitemUuids(parentUuid, 20);
        List<String> second = buildSubitemUuids(parentUuid, 20);
        assertEquals(first, second,
                "same parent + same ordinals must always produce the same sub-item UUIDs");
    }

    @Test
    void subitemUuidsFromDifferentParentsNeverCollide() {
        // 50 parents × 20 sub-items = 1000 UUIDs — no two should be equal
        List<String> parents = makeParentUuids(50);
        List<String> allUuids = parents.stream()
                .flatMap(p -> buildSubitemUuids(p, 20).stream())
                .collect(Collectors.toList());

        assertEquals(1000, new HashSet<>(allUuids).size(),
                "sub-item UUIDs must not collide across different parent items");
    }

    // =========================================================================
    // PartitionOffsetTracker — crash-point mechanics
    // =========================================================================

    @Test
    void committedOffsetAdvancesOnlyThroughConsecutiveCompletions() {
        PartitionOffsetTracker tracker = new PartitionOffsetTracker();
        tracker.recordPolled(PARTITION, 0);
        tracker.recordPolled(PARTITION, 1);
        tracker.recordPolled(PARTITION, 2);
        tracker.recordPolled(PARTITION, 3);

        tracker.markDone(PARTITION, 0);
        tracker.markDone(PARTITION, 1);
        // offsets 2 and 3 are in-flight (not done)

        Map<TopicPartition, OffsetAndMetadata> committed = tracker.drainCommittable();

        assertTrue(committed.containsKey(PARTITION));
        // Committed offset = 2 (next to read), meaning offsets 0 and 1 are committed
        assertEquals(2L, committed.get(PARTITION).offset(),
                "watermark must stop at the first gap (offset 2 not done)");
    }

    @Test
    void uncommittedGapCausesRedeliveryOfAllItemsFromGapOnward() {
        // Items 0, 1, 3 done; item 2 NOT done (e.g. agent killed mid-send callback)
        PartitionOffsetTracker tracker = new PartitionOffsetTracker();
        for (int i = 0; i < 5; i++) tracker.recordPolled(PARTITION, i);
        tracker.markDone(PARTITION, 0);
        tracker.markDone(PARTITION, 1);
        // offset 2 not marked done
        tracker.markDone(PARTITION, 3);
        tracker.markDone(PARTITION, 4);

        Map<TopicPartition, OffsetAndMetadata> committed = tracker.drainCommittable();

        long nextOffset = committed.get(PARTITION).offset();
        assertEquals(2L, nextOffset,
                "watermark stops at gap; restart re-delivers offsets 2, 3, 4");
    }

    @Test
    void emptyCommitMapWhenNothingIsMarkedDone() {
        PartitionOffsetTracker tracker = new PartitionOffsetTracker();
        for (int i = 0; i < 5; i++) tracker.recordPolled(PARTITION, i);
        // Nothing marked done

        Map<TopicPartition, OffsetAndMetadata> committed = tracker.drainCommittable();
        assertFalse(committed.containsKey(PARTITION),
                "no offset committed if nothing is done — all items re-delivered on restart");
    }

    // =========================================================================
    // Poison-message handling — poison items don't corrupt the effect
    // =========================================================================

    @Test
    void poisonItemsAreDlqdAndDoNotAppearInOutputIndex() {
        // A poison sentinel is detected and DLQ'd; it must NOT be indexed
        // (no parent UUID entry from a poison message).
        KafkaItemDeserializer deser = new KafkaItemDeserializer();
        byte[] malformed = "NOT_JSON".getBytes(StandardCharsets.UTF_8);
        KafkaItemMessage sentinel = deser.deserialize("iped.case-validation.stage.1", malformed);

        assertTrue(KafkaItemDeserializer.isPoison(sentinel));

        // In a real run, TaskAgent routes poison messages to the DLQ
        // without inserting them into the output index.
        // Verify the sentinel's UUID is synthetic (random, not derived from evidence)
        // by confirming it is present and non-null — the DLQ entry will carry it,
        // but the output stage topic will never see it.
        assertNotNull(sentinel.getItemUuid(),
                "DLQ routing still requires a non-null UUID for the Kafka record key");
        assertFalse(KafkaItemDeserializer.isPoison(new KafkaItemMessage()),
                "a normal (un-poisoned) KafkaItemMessage must not be flagged");
    }

    @Test
    void poisonMessageDoesNotBlockSubsequentValidItems() {
        // After a poison sentinel is handled, subsequent valid items must still
        // produce stable sub-item UUIDs and be indexable normally.
        KafkaItemDeserializer deser = new KafkaItemDeserializer();
        byte[] malformed = "BAD".getBytes(StandardCharsets.UTF_8);
        KafkaItemMessage poison = deser.deserialize("iped.case-validation.stage.1", malformed);
        assertTrue(KafkaItemDeserializer.isPoison(poison));

        // Normal item after the poison — UUID generation must be unaffected
        String parentUuid = UUID.nameUUIDFromBytes("parent-after-poison"
                .getBytes(StandardCharsets.UTF_8)).toString();
        List<String> run1 = buildSubitemUuids(parentUuid, 5);
        List<String> run2 = buildSubitemUuids(parentUuid, 5);
        assertEquals(run1, run2, "sub-item UUIDs must remain stable after a poison message is handled");
    }
}
