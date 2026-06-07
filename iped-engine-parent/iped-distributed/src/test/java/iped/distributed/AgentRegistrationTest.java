package iped.distributed;

import iped.distributed.coordinator.AgentRegistration;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

class AgentRegistrationTest {

    @Test
    void of_setsAgentId() {
        AgentRegistration reg = AgentRegistration.of("agent-1", "HashTask", 2, 4, "host1");
        assertEquals("agent-1", reg.getAgentId());
    }

    @Test
    void of_setsTaskType() {
        AgentRegistration reg = AgentRegistration.of("agent-1", "HashTask", 2, 4, "host1");
        assertEquals("HashTask", reg.getTaskType());
    }

    @Test
    void of_setsStageNumber() {
        AgentRegistration reg = AgentRegistration.of("agent-1", "HashTask", 3, 8, "host1");
        assertEquals(3, reg.getStageNumber());
    }

    @Test
    void of_setsMaxParallelItems() {
        AgentRegistration reg = AgentRegistration.of("agent-1", "HashTask", 2, 8, "host1");
        assertEquals(8, reg.getMaxParallelItems());
    }

    @Test
    void of_setsHostname() {
        AgentRegistration reg = AgentRegistration.of("agent-1", "HashTask", 2, 4, "worker-node-3");
        assertEquals("worker-node-3", reg.getHostname());
    }

    @Test
    void of_initialCurrentLoad_isZero() {
        AgentRegistration reg = AgentRegistration.of("agent-1", "HashTask", 2, 4, "host1");
        assertEquals(0, reg.getCurrentLoad());
    }

    @Test
    void of_initialFreeSlots_equalsMaxParallelItems() {
        AgentRegistration reg = AgentRegistration.of("agent-1", "HashTask", 2, 6, "host1");
        assertEquals(6, reg.getFreeSlots());
    }

    @Test
    void of_lastHeartbeat_isNotNull() {
        AgentRegistration reg = AgentRegistration.of("agent-1", "HashTask", 2, 4, "host1");
        assertNotNull(reg.getLastHeartbeat());
    }

    @Test
    void of_lastHeartbeat_isRecent() {
        Instant before = Instant.now().minusSeconds(5);
        AgentRegistration reg = AgentRegistration.of("agent-1", "HashTask", 2, 4, "host1");
        Instant after = Instant.now().plusSeconds(5);
        assertTrue(reg.getLastHeartbeat().isAfter(before));
        assertTrue(reg.getLastHeartbeat().isBefore(after));
    }

    @Test
    void setters_roundTrip() {
        AgentRegistration reg = new AgentRegistration();
        reg.setAgentId("a2");
        reg.setTaskType("IndexTask");
        reg.setStageNumber(5);
        reg.setHostname("server-9");
        reg.setMaxParallelItems(16);
        reg.setCurrentLoad(3);
        reg.setFreeSlots(13);

        assertEquals("a2", reg.getAgentId());
        assertEquals("IndexTask", reg.getTaskType());
        assertEquals(5, reg.getStageNumber());
        assertEquals("server-9", reg.getHostname());
        assertEquals(16, reg.getMaxParallelItems());
        assertEquals(3, reg.getCurrentLoad());
        assertEquals(13, reg.getFreeSlots());
    }

    @Test
    void setLastHeartbeat_roundTrip() {
        AgentRegistration reg = new AgentRegistration();
        Instant ts = Instant.parse("2024-01-15T10:30:00Z");
        reg.setLastHeartbeat(ts);
        assertEquals(ts, reg.getLastHeartbeat());
    }
}
