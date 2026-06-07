package iped.distributed;

import iped.distributed.coordinator.AgentAvailability;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class AgentAvailabilityTest {

    @Test
    void of_setsTaskType() {
        AgentAvailability a = AgentAvailability.of("HashTask", 2, 3, 12, 7);
        assertEquals("HashTask", a.getTaskType());
    }

    @Test
    void of_setsStageNumber() {
        AgentAvailability a = AgentAvailability.of("HashTask", 3, 3, 12, 7);
        assertEquals(3, a.getStageNumber());
    }

    @Test
    void of_setsTotalAgents() {
        AgentAvailability a = AgentAvailability.of("HashTask", 2, 5, 20, 10);
        assertEquals(5, a.getTotalAgents());
    }

    @Test
    void of_setsTotalSlots() {
        AgentAvailability a = AgentAvailability.of("HashTask", 2, 3, 12, 7);
        assertEquals(12, a.getTotalSlots());
    }

    @Test
    void of_setsFreeSlots() {
        AgentAvailability a = AgentAvailability.of("HashTask", 2, 3, 12, 7);
        assertEquals(7, a.getFreeSlots());
    }

    @Test
    void of_busySlots_isTotalMinusFree() {
        AgentAvailability a = AgentAvailability.of("HashTask", 2, 3, 12, 7);
        assertEquals(12 - 7, a.getBusySlots());
        assertEquals(5, a.getBusySlots());
    }

    @Test
    void of_allFree_busySlotsIsZero() {
        AgentAvailability a = AgentAvailability.of("HashTask", 1, 2, 8, 8);
        assertEquals(0, a.getBusySlots());
        assertEquals(8, a.getFreeSlots());
    }

    @Test
    void of_allBusy_freeSlotsIsZero() {
        AgentAvailability a = AgentAvailability.of("HashTask", 1, 2, 8, 0);
        assertEquals(8, a.getBusySlots());
        assertEquals(0, a.getFreeSlots());
    }

    @Test
    void setters_roundTrip() {
        AgentAvailability a = new AgentAvailability();
        a.setTaskType("IndexTask");
        a.setStageNumber(4);
        a.setTotalAgents(10);
        a.setTotalSlots(40);
        a.setFreeSlots(25);
        a.setBusySlots(15);

        assertEquals("IndexTask", a.getTaskType());
        assertEquals(4, a.getStageNumber());
        assertEquals(10, a.getTotalAgents());
        assertEquals(40, a.getTotalSlots());
        assertEquals(25, a.getFreeSlots());
        assertEquals(15, a.getBusySlots());
    }

    @Test
    void busySlots_consistencyWithTotalAndFree() {
        // Verify the invariant: busySlots = totalSlots - freeSlots holds via of()
        int totalSlots = 20;
        int freeSlots = 13;
        AgentAvailability a = AgentAvailability.of("T", 1, 2, totalSlots, freeSlots);
        assertEquals(totalSlots - freeSlots, a.getBusySlots());
    }
}
