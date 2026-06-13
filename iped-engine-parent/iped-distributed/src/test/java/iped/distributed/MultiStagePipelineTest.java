package iped.distributed;

import iped.distributed.coordinator.CaseCompletionMonitor;
import iped.distributed.coordinator.CaseLifecycleManager;
import iped.distributed.kafka.KafkaItemMessage;
import iped.distributed.kafka.TopicProvisioner;
import iped.distributed.status.ItemStatusEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Phase 8 — Multi-stage pipeline correctness.
 *
 * <p>Verifies the behaviour of {@link CaseCompletionMonitor} across a 3-task pipeline
 * ({@code HashTask → SignatureTask → IndexTask} at stages 0, 1, 2).  The key invariant
 * is that {@code isFinalStage} fires {@code completedFinal} <em>only</em> at stage 2
 * (IndexTask); completions at stages 0 and 1 do not count toward case completion.
 *
 * <p>All tests are broker-free.
 */
class MultiStagePipelineTest {

    private static final String CASE_ID    = "stage-case";
    private static final String HASH       = "HashTask";
    private static final String SIGNATURE  = "SignatureTask";
    private static final String INDEX      = "IndexTask";
    /** Ordered tasks → stages: HASH=0, SIGNATURE=1, INDEX=2. */
    private static final List<String> TASKS = List.of(HASH, SIGNATURE, INDEX);

    private CaseLifecycleManager  lifecycle;
    private List<ItemStatusEvent> published;
    private CaseCompletionMonitor monitor;

    @BeforeEach
    void setUp() {
        TopicProvisioner tp = new TopicProvisioner("localhost:0") {
            @Override public void provisionCase(String id, int n, int p, short r) {}
        };
        lifecycle = new CaseLifecycleManager(tp);
        lifecycle.startCase(CASE_ID, TASKS, 4, (short) 1);

        published = new ArrayList<>();
        monitor   = new CaseCompletionMonitor(lifecycle, published::add, 3600);
    }

    // ── Stage routing ─────────────────────────────────────────────────────────

    @Test
    void stage0Completion_doesNotTriggerCaseComplete() {
        monitor.onEvent(ItemStatusEvent.discovered(msg("item-1", 0)));
        monitor.onEvent(ItemStatusEvent.started(msg("item-1", 0), HASH));
        monitor.onEvent(ItemStatusEvent.completed(msg("item-1", 0), HASH, 10L));

        assertEquals(0, monitor.completedFinalCount(CASE_ID),
                "stage-0 (HashTask) completion must not count toward completedFinal");
        assertFalse(monitor.isCompleted(CASE_ID),
                "case must not be complete after only the first stage");
    }

    @Test
    void stage1Completion_doesNotTriggerCaseComplete() {
        monitor.onEvent(ItemStatusEvent.discovered(msg("item-1", 0)));
        // Stage 1 (SignatureTask)
        monitor.onEvent(ItemStatusEvent.started(msg("item-1", 1), SIGNATURE));
        monitor.onEvent(ItemStatusEvent.completed(msg("item-1", 1), SIGNATURE, 10L));

        assertEquals(0, monitor.completedFinalCount(CASE_ID),
                "stage-1 (SignatureTask) completion must not count toward completedFinal");
        assertFalse(monitor.isCompleted(CASE_ID));
    }

    @Test
    void stage2Completion_triggersCompletedFinalAndCaseComplete() {
        monitor.onEvent(ItemStatusEvent.discovered(msg("item-1", 0)));
        // Only the final stage matters for completedFinal
        monitor.onEvent(ItemStatusEvent.started(msg("item-1", 2), INDEX));
        monitor.onEvent(ItemStatusEvent.completed(msg("item-1", 2), INDEX, 10L));

        assertEquals(1, monitor.completedFinalCount(CASE_ID),
                "stage-2 (IndexTask) completion must increment completedFinal");
        assertTrue(monitor.isCompleted(CASE_ID),
                "case must be complete once all discovered items pass the final stage");
    }

    @Test
    void fullPipelineRun_singleItem_completesAtFinalStage() {
        monitor.onEvent(ItemStatusEvent.discovered(msg("item-1", 0)));

        // Stage 0
        monitor.onEvent(ItemStatusEvent.started(msg("item-1", 0), HASH));
        monitor.onEvent(ItemStatusEvent.completed(msg("item-1", 0), HASH, 5L));
        assertFalse(monitor.isCompleted(CASE_ID), "not done after stage 0");

        // Stage 1
        monitor.onEvent(ItemStatusEvent.started(msg("item-1", 1), SIGNATURE));
        monitor.onEvent(ItemStatusEvent.completed(msg("item-1", 1), SIGNATURE, 5L));
        assertFalse(monitor.isCompleted(CASE_ID), "not done after stage 1");

        // Stage 2 (final)
        monitor.onEvent(ItemStatusEvent.started(msg("item-1", 2), INDEX));
        monitor.onEvent(ItemStatusEvent.completed(msg("item-1", 2), INDEX, 5L));
        assertTrue(monitor.isCompleted(CASE_ID), "done after stage 2");

        assertEquals(1, caseCompletedCount(), "exactly one CASE_COMPLETED event");
    }

    @Test
    void fullPipelineRun_multipleItems_caseCompletesOnlyWhenAllPassFinalStage() {
        // 3 items discovered up front
        for (int i = 0; i < 3; i++) monitor.onEvent(ItemStatusEvent.discovered(msg("item-" + i, 0)));

        // All items through stages 0 and 1
        for (int i = 0; i < 3; i++) {
            monitor.onEvent(ItemStatusEvent.started(msg("item-" + i, 0), HASH));
            monitor.onEvent(ItemStatusEvent.completed(msg("item-" + i, 0), HASH, 5L));
            monitor.onEvent(ItemStatusEvent.started(msg("item-" + i, 1), SIGNATURE));
            monitor.onEvent(ItemStatusEvent.completed(msg("item-" + i, 1), SIGNATURE, 5L));
        }
        assertFalse(monitor.isCompleted(CASE_ID), "2 stages done but not the final one");
        assertEquals(0, monitor.completedFinalCount(CASE_ID));

        // Items 0 and 1 reach final stage
        for (int i = 0; i < 2; i++) {
            monitor.onEvent(ItemStatusEvent.started(msg("item-" + i, 2), INDEX));
            monitor.onEvent(ItemStatusEvent.completed(msg("item-" + i, 2), INDEX, 5L));
        }
        assertFalse(monitor.isCompleted(CASE_ID),
                "case still open — item-2 has not passed the final stage");
        assertEquals(2, monitor.completedFinalCount(CASE_ID));

        // Item 2 reaches final stage
        monitor.onEvent(ItemStatusEvent.started(msg("item-2", 2), INDEX));
        monitor.onEvent(ItemStatusEvent.completed(msg("item-2", 2), INDEX, 5L));
        assertTrue(monitor.isCompleted(CASE_ID), "all 3 items through final stage — case complete");
        assertEquals(1, caseCompletedCount());
    }

    // ── In-flight tracking across stages ─────────────────────────────────────

    @Test
    void inFlight_trackedPerTask_notPerStageNumber() {
        monitor.onEvent(ItemStatusEvent.discovered(msg("item-1", 0)));

        // Start at stage 0
        monitor.onEvent(ItemStatusEvent.started(msg("item-1", 0), HASH));
        assertEquals(1, monitor.inFlightCount(CASE_ID), "in-flight at stage 0");

        // Complete stage 0 — leaves in-flight
        monitor.onEvent(ItemStatusEvent.completed(msg("item-1", 0), HASH, 5L));
        assertEquals(0, monitor.inFlightCount(CASE_ID), "no longer in-flight after stage 0 done");

        // Start at stage 1
        monitor.onEvent(ItemStatusEvent.started(msg("item-1", 1), SIGNATURE));
        assertEquals(1, monitor.inFlightCount(CASE_ID), "in-flight at stage 1");
    }

    @Test
    void inFlight_clearedOnSkip() {
        monitor.onEvent(ItemStatusEvent.discovered(msg("item-1", 0)));
        monitor.onEvent(ItemStatusEvent.started(msg("item-1", 0), HASH));
        monitor.onEvent(ItemStatusEvent.skipped(msg("item-1", 0), HASH));

        assertEquals(0, monitor.inFlightCount(CASE_ID),
                "skipped item must be removed from in-flight");
    }

    @Test
    void inFlight_clearedOnError() {
        monitor.onEvent(ItemStatusEvent.discovered(msg("item-1", 0)));
        monitor.onEvent(ItemStatusEvent.started(msg("item-1", 0), HASH));
        monitor.onEvent(ItemStatusEvent.error(msg("item-1", 0), HASH, 5L,
                new RuntimeException("hash failed")));

        assertEquals(0, monitor.inFlightCount(CASE_ID),
                "errored item must be removed from in-flight");
        assertEquals(1, monitor.failedCount(CASE_ID),
                "error must be counted in failedCount");
    }

    // ── Timeout at intermediate stage ─────────────────────────────────────────

    @Test
    void timeout_atIntermediateStage_doesNotIncrementCompletedFinal() {
        // 0-second timeout so the item times out immediately
        CaseCompletionMonitor zeroTimeout = new CaseCompletionMonitor(lifecycle, published::add, 0);

        zeroTimeout.onEvent(ItemStatusEvent.discovered(msg("item-1", 0)));
        zeroTimeout.onEvent(ItemStatusEvent.started(msg("item-1", 0), HASH));

        // Sweep with a future instant — item's startedAt is already in the past
        zeroTimeout.sweepTimeouts(Instant.now().plusSeconds(1));

        // TIMEOUT event published
        long timeouts = published.stream()
                .filter(e -> e.getType() == ItemStatusEvent.Type.TIMEOUT).count();
        assertEquals(1, timeouts, "one TIMEOUT must be emitted for the stuck item");

        // completedFinal must NOT have been incremented
        assertEquals(0, zeroTimeout.completedFinalCount(CASE_ID),
                "timeout at stage 0 must not count toward completedFinal");
        assertFalse(zeroTimeout.isCompleted(CASE_ID),
                "case must not be considered complete after a mid-pipeline timeout");
    }

    // ── Multi-case isolation ──────────────────────────────────────────────────

    @Test
    void twoSeparateCases_finalStageCountsAreIsolated() {
        String caseB = "stage-case-B";
        TopicProvisioner tp2 = new TopicProvisioner("localhost:0") {
            @Override public void provisionCase(String id, int n, int p, short r) {}
        };
        // Reuse the same lifecycle manager for both cases
        lifecycle.startCase(caseB, TASKS, 4, (short) 1);

        monitor.onEvent(ItemStatusEvent.discovered(itemMsgFor(CASE_ID, "a-item", 0)));
        monitor.onEvent(ItemStatusEvent.discovered(itemMsgFor(caseB, "b-item", 0)));

        // Case A: item goes through all 3 stages
        monitor.onEvent(ItemStatusEvent.started(msg("a-item", 2), INDEX));
        monitor.onEvent(ItemStatusEvent.completed(msg("a-item", 2), INDEX, 5L));

        // Case B: item is still mid-pipeline
        assertEquals(1, monitor.completedFinalCount(CASE_ID));
        assertEquals(0, monitor.completedFinalCount(caseB),
                "case B completedFinal must not be contaminated by case A's final completion");
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    /** Builds a message for {@link #CASE_ID}. */
    private static KafkaItemMessage msg(String uuid, int stage) {
        return itemMsgFor(CASE_ID, uuid, stage);
    }

    private static KafkaItemMessage itemMsgFor(String caseId, String uuid, int stage) {
        KafkaItemMessage m = new KafkaItemMessage();
        m.setCaseId(caseId);
        m.setItemUuid(uuid);
        m.setPipelineStage(stage);
        return m;
    }

    private long caseCompletedCount() {
        return published.stream()
                .filter(e -> e.getType() == ItemStatusEvent.Type.CASE_COMPLETED).count();
    }
}
