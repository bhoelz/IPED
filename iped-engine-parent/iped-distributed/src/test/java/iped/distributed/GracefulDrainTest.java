package iped.distributed;

import iped.distributed.kafka.PartitionOffsetTracker;
import org.apache.kafka.clients.consumer.OffsetAndMetadata;
import org.apache.kafka.common.TopicPartition;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for graceful-drain correctness.
 *
 * <h2>The bug this addresses</h2>
 * <p>Before the fix, {@code TaskAgent}'s shutdown sequence was:
 * <pre>
 *   executor.awaitTermination(60s)
 *   drainCommittable()       ← misses callbacks not yet fired
 *   commitSync(remaining)
 *   consumer.close()
 *   producer.close()         ← flush happens here, too late
 * </pre>
 * Items whose {@code producer.send} callback had not yet fired by the time
 * {@code executor.awaitTermination} returned would have their {@code markDone} call
 * arrive <em>after</em> the final {@code commitSync}, so their offsets were never
 * committed — causing unnecessary re-delivery on restart.
 *
 * <h2>The fix</h2>
 * <p>Insert {@code producer.flush()} between {@code executor.awaitTermination} and
 * {@code drainCommittable()} in all exit paths.  The flush blocks until every
 * pending send and its callback completes, so all {@code markDone} calls fire before
 * the final commit.
 *
 * <h2>Graceful drain</h2>
 * <p>{@code drain()} sets a flag that causes the poll loop to:
 * pause Kafka partitions (stop accepting new records), wait for in-flight items to
 * complete, call flush, then exit — so items in-flight at drain time finish cleanly.
 *
 * <p>None of these tests require a running Kafka broker.
 */
class GracefulDrainTest {

    private static final TopicPartition P0 = new TopicPartition("iped.case1.stage.1", 0);
    private static final TopicPartition P1 = new TopicPartition("iped.case1.stage.1", 1);

    // =========================================================================
    // Core ordering invariant: flush before drainCommittable
    // =========================================================================

    /**
     * Demonstrates the pre-fix bug: calling {@code drainCommittable()} before the
     * producer callback fires misses the completed offset.
     */
    @Test
    void withoutFlush_asyncCallbackMissedByCommit() {
        PartitionOffsetTracker tracker = new PartitionOffsetTracker();
        tracker.recordPolled(P0, 0);
        tracker.recordPolled(P0, 1);

        // Executor task completes synchronously for offset 0
        tracker.markDone(P0, 0);

        // Executor task for offset 1 has returned (inFlight=0), but the
        // producer callback hasn't fired yet — markDone(1) not called.

        // Hard stop without flush: drainCommittable called while callback is pending
        Map<TopicPartition, OffsetAndMetadata> prematureCommit = tracker.drainCommittable();

        assertEquals(1L, prematureCommit.get(P0).offset(),
                "without flush: only offset 0 committed; offset 1 callback not yet fired");

        // Now the callback fires (simulating producer.flush() completing)
        tracker.markDone(P0, 1);

        // Too late — this offset will not be in the already-sent commitSync
        Map<TopicPartition, OffsetAndMetadata> tooLate = tracker.drainCommittable();
        assertEquals(2L, tooLate.get(P0).offset(),
                "the offset that arrived late is visible in the next drain, "
                + "but the prior commitSync already ran without it");
    }

    /**
     * The fix: calling {@code drainCommittable()} AFTER flush ensures all callbacks
     * are captured in a single commit.
     */
    @Test
    void withFlush_asyncCallbackCapturedBeforeCommit() {
        PartitionOffsetTracker tracker = new PartitionOffsetTracker();
        for (int i = 0; i < 5; i++) tracker.recordPolled(P0, i);

        // Synchronous completions (from executor threads)
        tracker.markDone(P0, 0);
        tracker.markDone(P0, 1);
        tracker.markDone(P0, 2);

        // "producer.flush()" fires the pending async callbacks for 3 and 4
        tracker.markDone(P0, 3);
        tracker.markDone(P0, 4);

        // NOW call drainCommittable — all 5 offsets captured in one commit
        Map<TopicPartition, OffsetAndMetadata> committed = tracker.drainCommittable();
        assertEquals(5L, committed.get(P0).offset(),
                "with flush: all 5 offsets committed in a single pass");
    }

    // =========================================================================
    // Flush before drain — multi-threaded simulation
    // =========================================================================

    /**
     * Simulates the producer callback arriving on a separate thread shortly after the
     * executor task returns.  {@code drainCommittable()} called after a brief wait
     * (simulating flush) captures it; called immediately (no flush) misses it.
     */
    @Test
    void flushMakesAsyncMarkDoneVisibleToCommit() throws InterruptedException {
        PartitionOffsetTracker tracker = new PartitionOffsetTracker();
        tracker.recordPolled(P0, 0);
        tracker.recordPolled(P0, 1);
        tracker.markDone(P0, 0);

        // Simulate async producer callback firing 50ms after executor task returns
        CountDownLatch callbackFired = new CountDownLatch(1);
        ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
        scheduler.schedule(() -> {
            tracker.markDone(P0, 1);
            callbackFired.countDown();
        }, 50, TimeUnit.MILLISECONDS);

        // Without flush (immediate drain): only offset 0 captured
        Map<TopicPartition, OffsetAndMetadata> withoutFlush = tracker.drainCommittable();
        assertEquals(1L, withoutFlush.get(P0).offset(),
                "immediate drain misses offset 1 whose callback has not yet fired");

        // Simulate flush: wait for all callbacks
        assertTrue(callbackFired.await(2, TimeUnit.SECONDS), "callback must fire within 2s");

        // After flush: offset 1 is now committed
        Map<TopicPartition, OffsetAndMetadata> afterFlush = tracker.drainCommittable();
        assertEquals(2L, afterFlush.get(P0).offset(),
                "after flush: offset 1 committed");

        scheduler.shutdown();
    }

    // =========================================================================
    // Drain-flag mechanics
    // =========================================================================

    /**
     * Simulates the consumer-loop drain check: once {@code draining=true} and
     * {@code inFlight=0}, the loop sets {@code running=false} and exits.
     */
    @Test
    void drainFlagWithZeroInFlightExitsLoop() throws InterruptedException {
        AtomicBoolean running  = new AtomicBoolean(true);
        AtomicBoolean draining = new AtomicBoolean(false);
        AtomicInteger inFlight = new AtomicInteger(2);

        // Simulates the poll loop: checks drain condition each iteration
        CountDownLatch exited = new CountDownLatch(1);
        Thread loopThread = new Thread(() -> {
            while (running.get()) {
                if (draining.get() && inFlight.get() == 0) {
                    running.set(false);   // mirrors: flushProducer(); break;
                }
                try { Thread.sleep(10); } catch (InterruptedException e) { break; }
            }
            exited.countDown();
        }, "mock-consumer-loop");
        loopThread.start();

        // Signal drain
        draining.set(true);
        Thread.sleep(50);
        assertTrue(running.get(), "still running with 2 items in flight");

        // Complete first item
        inFlight.decrementAndGet();
        Thread.sleep(50);
        assertTrue(running.get(), "still running with 1 item in flight");

        // Complete last item → loop should exit
        inFlight.decrementAndGet();
        assertTrue(exited.await(2, TimeUnit.SECONDS),
                "loop must exit once inFlight reaches 0 during drain");
        assertFalse(running.get(), "running must be false after drain completes");
    }

    /**
     * A hard stop (running=false) exits without waiting for inFlight.
     */
    @Test
    void hardStopExitsImmediatelyRegardlessOfInFlight() throws InterruptedException {
        AtomicBoolean running  = new AtomicBoolean(true);
        AtomicInteger inFlight = new AtomicInteger(5);

        CountDownLatch exited = new CountDownLatch(1);
        Thread loopThread = new Thread(() -> {
            while (running.get()) {
                try { Thread.sleep(10); } catch (InterruptedException e) { break; }
            }
            exited.countDown();
        }, "mock-consumer-loop");
        loopThread.start();

        // Hard stop: exit immediately even though inFlight > 0
        running.set(false);
        assertTrue(exited.await(1, TimeUnit.SECONDS),
                "loop must exit immediately after stop() regardless of inFlight count");
        assertEquals(5, inFlight.get(), "in-flight items not awaited by hard stop");
    }

    /**
     * After drain completes, new records must not be submitted to the executor.
     * The drain flag doubles as a "no new intake" gate in the record dispatch loop.
     */
    @Test
    void drainingFlagBlocksNewRecordSubmission() {
        AtomicBoolean draining = new AtomicBoolean(false);
        AtomicInteger submitted = new AtomicInteger(0);

        // Simulate the record dispatch: only submit if not draining
        Runnable dispatchRecord = () -> {
            if (!draining.get()) {
                submitted.incrementAndGet();
            }
        };

        // Before drain: records accepted
        dispatchRecord.run();
        dispatchRecord.run();
        assertEquals(2, submitted.get());

        // After drain signal: no new records accepted
        draining.set(true);
        dispatchRecord.run();
        dispatchRecord.run();
        assertEquals(2, submitted.get(), "no new records accepted during drain");
    }

    // =========================================================================
    // Multi-partition drain ordering
    // =========================================================================

    @Test
    void drainCommitsAllPartitionsAfterFlush() {
        PartitionOffsetTracker tracker = new PartitionOffsetTracker();

        // Two partitions with 3 offsets each
        for (int i = 0; i < 3; i++) tracker.recordPolled(P0, i);
        for (int i = 0; i < 3; i++) tracker.recordPolled(P1, i);

        // All complete synchronously (simulating flush having fired all callbacks)
        for (int i = 0; i < 3; i++) tracker.markDone(P0, i);
        for (int i = 0; i < 3; i++) tracker.markDone(P1, i);

        Map<TopicPartition, OffsetAndMetadata> committed = tracker.drainCommittable();

        assertEquals(2, committed.size(), "both partitions must appear in the commit map");
        assertEquals(3L, committed.get(P0).offset(), "P0: next offset to read = 3");
        assertEquals(3L, committed.get(P1).offset(), "P1: next offset to read = 3");
    }

    @Test
    void partialFlush_onlyConsecutiveCompletionsCommitted() {
        PartitionOffsetTracker tracker = new PartitionOffsetTracker();
        for (int i = 0; i < 5; i++) tracker.recordPolled(P0, i);

        // Offsets 0, 1 done; offset 2 still pending (callback not yet fired);
        // offsets 3, 4 done — but gap at 2 prevents them from advancing the watermark
        tracker.markDone(P0, 0);
        tracker.markDone(P0, 1);
        tracker.markDone(P0, 3);
        tracker.markDone(P0, 4);

        Map<TopicPartition, OffsetAndMetadata> committed = tracker.drainCommittable();
        assertEquals(2L, committed.get(P0).offset(),
                "watermark stops at gap: offset 2 not done, so 3 and 4 also not committable");

        // Callback for offset 2 fires (late flush)
        tracker.markDone(P0, 2);

        Map<TopicPartition, OffsetAndMetadata> afterLateFlush = tracker.drainCommittable();
        assertEquals(5L, afterLateFlush.get(P0).offset(),
                "after gap filled: all 5 offsets committable");
    }

    // =========================================================================
    // Drain idempotency — calling drain() multiple times is safe
    // =========================================================================

    @Test
    void multipleCallsToDrainAreIdempotent() throws InterruptedException {
        AtomicBoolean draining = new AtomicBoolean(false);
        AtomicBoolean running  = new AtomicBoolean(true);
        AtomicInteger inFlight = new AtomicInteger(0);

        CountDownLatch exited = new CountDownLatch(1);
        Thread loopThread = new Thread(() -> {
            while (running.get()) {
                if (draining.get() && inFlight.get() == 0) {
                    running.set(false);
                }
                try { Thread.sleep(5); } catch (InterruptedException e) { break; }
            }
            exited.countDown();
        }, "mock-loop-idempotent");
        loopThread.start();

        // Call drain multiple times (e.g. SIGTERM received twice)
        draining.set(true);
        draining.set(true);
        draining.set(true);

        assertTrue(exited.await(1, TimeUnit.SECONDS), "loop exits cleanly despite multiple drain calls");
        assertFalse(running.get());
    }

    // =========================================================================
    // Shutdown ordering — the final commit captures all completed offsets
    // =========================================================================

    /**
     * Full shutdown path simulation:
     * 1. awaitTermination (all executor tasks returned)
     * 2. flushProducer     (all async callbacks fired → all markDone called)
     * 3. drainCommittable  (all committable offsets captured)
     * 4. commitSync
     *
     * Verifies that the offset captured in step 3 includes callbacks that fired
     * during step 2 (i.e., after executor tasks returned but before flush completed).
     */
    @Test
    void shutdownSequenceCapturesPostExecutorCallbacks() {
        PartitionOffsetTracker tracker = new PartitionOffsetTracker();
        for (int i = 0; i < 4; i++) tracker.recordPolled(P0, i);

        // Step 1: executor tasks complete (in practice these happen inside processRecord's finally)
        tracker.markDone(P0, 0);
        tracker.markDone(P0, 1);
        // Offsets 2 and 3: executor tasks returned but producer callbacks not yet fired

        // Step 2: producer.flush() — callbacks fire
        tracker.markDone(P0, 2);
        tracker.markDone(P0, 3);

        // Step 3: drainCommittable
        Map<TopicPartition, OffsetAndMetadata> toCommit = tracker.drainCommittable();

        // Step 4: commitSync(toCommit) — should commit all 4 offsets
        assertEquals(4L, toCommit.get(P0).offset(),
                "all 4 offsets captured after flush precedes drainCommittable");
    }

    @Test
    void shutdownWithoutFlush_missesPostExecutorCallbacks() {
        PartitionOffsetTracker tracker = new PartitionOffsetTracker();
        for (int i = 0; i < 4; i++) tracker.recordPolled(P0, i);

        // Executor tasks complete — only 0 and 1 have fired their callbacks so far
        tracker.markDone(P0, 0);
        tracker.markDone(P0, 1);
        // Offsets 2 and 3: executor returned but callbacks not yet fired

        // drainCommittable WITHOUT prior flush — callbacks for 2 and 3 arrive later
        Map<TopicPartition, OffsetAndMetadata> premature = tracker.drainCommittable();
        assertEquals(2L, premature.get(P0).offset(),
                "without flush: only 2 offsets committed; 2 and 3 arrive too late");

        // These arrive after commitSync has already run — too late
        tracker.markDone(P0, 2);
        tracker.markDone(P0, 3);

        // They ARE in the tracker now but the window to commit has closed
        Map<TopicPartition, OffsetAndMetadata> afterClose = tracker.drainCommittable();
        assertEquals(4L, afterClose.get(P0).offset(),
                "offsets 2 and 3 are tracked but would require a second commitSync pass "
                + "— which the original shutdown path did not perform");
    }
}
