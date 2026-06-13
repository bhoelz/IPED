package iped.distributed;

import iped.distributed.scheduler.CasePriority;
import iped.distributed.scheduler.CaseScheduler;
import iped.distributed.scheduler.SchedulableCase;
import iped.distributed.scheduler.SlotAssignment;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for {@link CaseScheduler}: strict priority across classes, round-robin within a
 * class, demand caps, and the no-preemption-of-free-only contract.
 *
 * <p>Pure logic — no Kafka broker or coordinator required.
 */
class CaseSchedulerTest {

    private CaseScheduler scheduler;

    @BeforeEach
    void setup() {
        scheduler = new CaseScheduler();
    }

    private static Map<String, Integer> asMap(List<SlotAssignment> assignments) {
        return assignments.stream()
                .collect(Collectors.toMap(SlotAssignment::caseId, SlotAssignment::slots));
    }

    private static int totalSlots(List<SlotAssignment> assignments) {
        return assignments.stream().mapToInt(SlotAssignment::slots).sum();
    }

    // =========================================================================
    // Empty / degenerate inputs
    // =========================================================================

    @Test
    void noFreeSlotsProducesNoAssignments() {
        var cases = List.of(SchedulableCase.of("c1", CasePriority.URGENT, 10));
        assertTrue(scheduler.schedule(cases, 0).isEmpty());
        assertTrue(scheduler.schedule(cases, -5).isEmpty());
    }

    @Test
    void noCasesProducesNoAssignments() {
        assertTrue(scheduler.schedule(List.of(), 8).isEmpty());
        assertTrue(scheduler.schedule(null, 8).isEmpty());
    }

    @Test
    void casesWithNoPendingWorkAreSkipped() {
        var cases = List.of(
                SchedulableCase.of("c1", CasePriority.URGENT, 0),
                SchedulableCase.of("c2", CasePriority.NORMAL, 0));
        assertTrue(scheduler.schedule(cases, 8).isEmpty());
    }

    // =========================================================================
    // Strict priority across classes
    // =========================================================================

    @Test
    void urgentCaseTakesAllSlotsBeforeNormal() {
        var cases = List.of(
                SchedulableCase.of("normal", CasePriority.NORMAL, 100),
                SchedulableCase.of("urgent", CasePriority.URGENT, 100));
        List<SlotAssignment> plan = scheduler.schedule(cases, 8);

        Map<String, Integer> m = asMap(plan);
        assertEquals(8, m.getOrDefault("urgent", 0),
                "all 8 slots go to the urgent case");
        assertNull(m.get("normal"), "normal case gets nothing while urgent has pending work");
    }

    @Test
    void higherPriorityServedFirstThenSpillsToLower() {
        // urgent wants 3, then remaining 5 slots go to normal
        var cases = List.of(
                SchedulableCase.of("urgent", CasePriority.URGENT, 3),
                SchedulableCase.of("normal", CasePriority.NORMAL, 100));
        List<SlotAssignment> plan = scheduler.schedule(cases, 8);

        Map<String, Integer> m = asMap(plan);
        assertEquals(3, m.get("urgent"), "urgent capped at its demand of 3");
        assertEquals(5, m.get("normal"), "remaining 5 slots spill to normal");
    }

    @Test
    void fullPriorityLadderServedInOrder() {
        var cases = List.of(
                SchedulableCase.of("low",    CasePriority.LOW, 10),
                SchedulableCase.of("normal", CasePriority.NORMAL, 10),
                SchedulableCase.of("high",   CasePriority.HIGH, 10),
                SchedulableCase.of("urgent", CasePriority.URGENT, 10));
        // 25 slots: urgent 10, high 10, normal 5, low 0
        List<SlotAssignment> plan = scheduler.schedule(cases, 25);
        Map<String, Integer> m = asMap(plan);
        assertEquals(10, m.get("urgent"));
        assertEquals(10, m.get("high"));
        assertEquals(5, m.get("normal"));
        assertNull(m.get("low"), "low priority starved when higher classes consume all slots");
    }

    @Test
    void assignmentsOrderedByPriorityDescending() {
        var cases = List.of(
                SchedulableCase.of("normal", CasePriority.NORMAL, 5),
                SchedulableCase.of("urgent", CasePriority.URGENT, 5),
                SchedulableCase.of("high",   CasePriority.HIGH, 5));
        List<SlotAssignment> plan = scheduler.schedule(cases, 15);
        assertEquals(List.of("urgent", "high", "normal"),
                plan.stream().map(SlotAssignment::caseId).toList(),
                "output ordered highest priority first");
    }

    // =========================================================================
    // Round-robin within a priority class
    // =========================================================================

    @Test
    void equalPrioritySharesEvenlyRoundRobin() {
        var cases = List.of(
                SchedulableCase.of("a", CasePriority.NORMAL, 100),
                SchedulableCase.of("b", CasePriority.NORMAL, 100));
        List<SlotAssignment> plan = scheduler.schedule(cases, 8);
        Map<String, Integer> m = asMap(plan);
        assertEquals(4, m.get("a"));
        assertEquals(4, m.get("b"), "8 slots split evenly across two equal-priority cases");
    }

    @Test
    void roundRobinRemainderGoesToFirstByCaseId() {
        var cases = List.of(
                SchedulableCase.of("b", CasePriority.NORMAL, 100),
                SchedulableCase.of("a", CasePriority.NORMAL, 100),
                SchedulableCase.of("c", CasePriority.NORMAL, 100));
        // 7 slots, 3 cases → 3,2,2 with the extra going to the first by caseId order (a)
        List<SlotAssignment> plan = scheduler.schedule(cases, 7);
        Map<String, Integer> m = asMap(plan);
        assertEquals(3, m.get("a"), "remainder slot goes to the first case in caseId order");
        assertEquals(2, m.get("b"));
        assertEquals(2, m.get("c"));
        assertEquals(7, totalSlots(plan));
    }

    @Test
    void roundRobinRespectsPerCaseDemand() {
        // 'small' only wants 1; the rest should overflow to 'big' within the same class
        var cases = List.of(
                SchedulableCase.of("big",   CasePriority.NORMAL, 100),
                SchedulableCase.of("small", CasePriority.NORMAL, 1));
        List<SlotAssignment> plan = scheduler.schedule(cases, 8);
        Map<String, Integer> m = asMap(plan);
        assertEquals(1, m.get("small"), "small case capped at its demand of 1");
        assertEquals(7, m.get("big"), "the rest go to big");
    }

    // =========================================================================
    // Demand caps / no over-assignment
    // =========================================================================

    @Test
    void neverAssignsMoreThanTotalDemand() {
        var cases = List.of(
                SchedulableCase.of("c1", CasePriority.URGENT, 2),
                SchedulableCase.of("c2", CasePriority.NORMAL, 3));
        // 100 free slots but only 5 units of work total
        List<SlotAssignment> plan = scheduler.schedule(cases, 100);
        assertEquals(5, totalSlots(plan), "assign only as much as there is work");
        assertEquals(2, asMap(plan).get("c1"));
        assertEquals(3, asMap(plan).get("c2"));
    }

    @Test
    void assignsExactlyFreeSlotsWhenDemandExceedsCapacity() {
        var cases = List.of(
                SchedulableCase.of("c1", CasePriority.NORMAL, 50),
                SchedulableCase.of("c2", CasePriority.NORMAL, 50));
        List<SlotAssignment> plan = scheduler.schedule(cases, 8);
        assertEquals(8, totalSlots(plan), "never exceed available free slots");
    }

    // =========================================================================
    // No-preemption contract (free slots only)
    // =========================================================================

    @Test
    void inFlightWorkDoesNotAffectAssignmentAndIsNeverReduced() {
        // A normal case already running 10 items; an urgent case arrives with pending work.
        var cases = List.of(
                new SchedulableCase("normal", CasePriority.NORMAL, 20, 10),
                new SchedulableCase("urgent", CasePriority.URGENT, 20, 0));
        List<SlotAssignment> plan = scheduler.schedule(cases, 4);

        // Only free slots are scheduled — the urgent case wins them; nothing in the
        // assignment reduces normal's 10 in-flight items (the scheduler returns additive
        // free-slot directions only).
        Map<String, Integer> m = asMap(plan);
        assertEquals(4, m.get("urgent"));
        assertNull(m.get("normal"));
        assertTrue(plan.stream().allMatch(a -> a.slots() > 0),
                "assignments are positive free-slot grants, never negative/preemptive");
    }

    // =========================================================================
    // Determinism
    // =========================================================================

    @Test
    void scheduleIsDeterministic() {
        var cases = List.of(
                SchedulableCase.of("c3", CasePriority.HIGH, 7),
                SchedulableCase.of("c1", CasePriority.HIGH, 7),
                SchedulableCase.of("c2", CasePriority.NORMAL, 7));
        List<SlotAssignment> a = scheduler.schedule(cases, 10);
        List<SlotAssignment> b = scheduler.schedule(cases, 10);
        assertEquals(asMap(a), asMap(b));
        assertEquals(a.stream().map(SlotAssignment::caseId).toList(),
                b.stream().map(SlotAssignment::caseId).toList());
    }

    // =========================================================================
    // Priority enum helper
    // =========================================================================

    @Test
    void priorityParseOrDefault() {
        assertEquals(CasePriority.URGENT, CasePriority.parseOrDefault("urgent"));
        assertEquals(CasePriority.HIGH, CasePriority.parseOrDefault("HIGH"));
        assertEquals(CasePriority.NORMAL, CasePriority.parseOrDefault(null));
        assertEquals(CasePriority.NORMAL, CasePriority.parseOrDefault(""));
        assertEquals(CasePriority.NORMAL, CasePriority.parseOrDefault("bogus"));
    }

    @Test
    void priorityWeightsAreOrdered() {
        assertTrue(CasePriority.URGENT.weight() > CasePriority.HIGH.weight());
        assertTrue(CasePriority.HIGH.weight() > CasePriority.NORMAL.weight());
        assertTrue(CasePriority.NORMAL.weight() > CasePriority.LOW.weight());
    }
}
