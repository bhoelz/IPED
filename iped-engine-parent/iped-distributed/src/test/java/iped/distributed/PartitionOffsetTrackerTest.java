package iped.distributed;

import iped.distributed.kafka.PartitionOffsetTracker;
import org.apache.kafka.clients.consumer.OffsetAndMetadata;
import org.apache.kafka.common.TopicPartition;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

class PartitionOffsetTrackerTest {

    private static final TopicPartition P0 = new TopicPartition("test-topic", 0);
    private static final TopicPartition P1 = new TopicPartition("test-topic", 1);

    private PartitionOffsetTracker tracker;

    @BeforeEach
    void setUp() {
        tracker = new PartitionOffsetTracker();
    }

    // -------------------------------------------------------------------------
    // Basic watermark advancement
    // -------------------------------------------------------------------------

    @Test
    void nothingCommittableBeforeAnyRecordPolled() {
        assertTrue(tracker.drainCommittable().isEmpty());
    }

    @Test
    void nothingCommittableWhileRecordPending() {
        tracker.recordPolled(P0, 0);
        assertTrue(tracker.drainCommittable().isEmpty());
    }

    @Test
    void singleRecordCommittableAfterMarkDone() {
        tracker.recordPolled(P0, 0);
        tracker.markDone(P0, 0);

        Map<TopicPartition, OffsetAndMetadata> result = tracker.drainCommittable();

        assertEquals(1, result.size());
        // commit position is lastCompletedOffset + 1
        assertEquals(1L, result.get(P0).offset());
    }

    @Test
    void consecutiveOffsetsAdvanceWatermarkInOneCall() {
        tracker.recordPolled(P0, 10);
        tracker.recordPolled(P0, 11);
        tracker.recordPolled(P0, 12);

        tracker.markDone(P0, 10);
        tracker.markDone(P0, 11);
        tracker.markDone(P0, 12);

        Map<TopicPartition, OffsetAndMetadata> result = tracker.drainCommittable();
        assertEquals(13L, result.get(P0).offset());
    }

    // -------------------------------------------------------------------------
    // Gap handling: watermark must NOT advance past a pending offset
    // -------------------------------------------------------------------------

    @Test
    void gapBlocksWatermarkAdvancement() {
        tracker.recordPolled(P0, 5);
        tracker.recordPolled(P0, 6);
        tracker.recordPolled(P0, 7);

        // Finish 6 and 7, but NOT 5
        tracker.markDone(P0, 6);
        tracker.markDone(P0, 7);

        // Nothing should be committable — offset 5 is the first pending and is not done
        assertTrue(tracker.drainCommittable().isEmpty(),
                "watermark must not skip over incomplete offset 5");
    }

    @Test
    void watermarkAdvancesOnlyToFirstGap() {
        tracker.recordPolled(P0, 0);
        tracker.recordPolled(P0, 1);
        tracker.recordPolled(P0, 2);

        tracker.markDone(P0, 0);
        // 1 is not done; 2 is done

        Map<TopicPartition, OffsetAndMetadata> result = tracker.drainCommittable();
        // Only offset 0 is a consecutive run from the start
        assertEquals(1L, result.get(P0).offset(), "should commit only through offset 0");
    }

    @Test
    void lateArrivalFillsGapAndAdvancesWatermarkOnNextDrain() {
        tracker.recordPolled(P0, 0);
        tracker.recordPolled(P0, 1);
        tracker.recordPolled(P0, 2);

        tracker.markDone(P0, 0);
        // drain advances to 1
        tracker.drainCommittable();

        // now offset 1 finishes
        tracker.markDone(P0, 1);
        tracker.markDone(P0, 2);

        Map<TopicPartition, OffsetAndMetadata> result = tracker.drainCommittable();
        assertEquals(3L, result.get(P0).offset());
    }

    // -------------------------------------------------------------------------
    // Multiple partitions are tracked independently
    // -------------------------------------------------------------------------

    @Test
    void twoPartitionsTrackedIndependently() {
        tracker.recordPolled(P0, 0);
        tracker.recordPolled(P1, 0);

        tracker.markDone(P0, 0);
        // P1 offset 0 still pending

        Map<TopicPartition, OffsetAndMetadata> result = tracker.drainCommittable();

        assertTrue(result.containsKey(P0), "P0 should be committable");
        assertFalse(result.containsKey(P1), "P1 should not be committable yet");
        assertEquals(1L, result.get(P0).offset());
    }

    // -------------------------------------------------------------------------
    // Drain clears internal state (no double-commit)
    // -------------------------------------------------------------------------

    @Test
    void drainIsIdempotentWhenNoNewCompletions() {
        tracker.recordPolled(P0, 3);
        tracker.markDone(P0, 3);

        Map<TopicPartition, OffsetAndMetadata> first = tracker.drainCommittable();
        Map<TopicPartition, OffsetAndMetadata> second = tracker.drainCommittable();

        assertEquals(1, first.size());
        assertTrue(second.isEmpty(), "second drain must not re-commit the same offset");
    }

    // -------------------------------------------------------------------------
    // Thread safety: concurrent markDone calls must not lose updates
    // -------------------------------------------------------------------------

    @Test
    void concurrentMarkDoneIsThreadSafe() throws InterruptedException {
        int count = 100;
        for (int i = 0; i < count; i++) {
            tracker.recordPolled(P0, i);
        }

        ExecutorService pool = Executors.newFixedThreadPool(8);
        CountDownLatch latch = new CountDownLatch(count);
        for (int i = 0; i < count; i++) {
            final int offset = i;
            pool.submit(() -> {
                tracker.markDone(P0, offset);
                latch.countDown();
            });
        }
        assertTrue(latch.await(5, TimeUnit.SECONDS), "all markDone calls should complete");
        pool.shutdown();

        Map<TopicPartition, OffsetAndMetadata> result = tracker.drainCommittable();
        assertEquals((long) count, result.get(P0).offset(),
                "all 100 offsets (0..99) must be committable after concurrent completions");
    }
}
