package iped.distributed;

import static org.junit.jupiter.api.Assertions.*;

import iped.distributed.coordinator.AgentAvailability;
import iped.distributed.coordinator.AgentRegistration;
import iped.distributed.coordinator.AgentRegistry;
import java.util.Map;
import org.junit.jupiter.api.Test;

class AgentRegistryTest {

  @Test
  void registerAndHeartbeat() {
    AgentRegistry registry = new AgentRegistry(30);

    AgentRegistration reg = AgentRegistration.of("agent-1", "HashTask", 2, 4, "host1");
    registry.register(reg);

    assertEquals(1, registry.liveAgents().size());
    assertEquals("HashTask", registry.liveAgents().get(0).getTaskType());
  }

  @Test
  void availabilityAggregatesSlots() {
    AgentRegistry registry = new AgentRegistry(30);

    registry.register(AgentRegistration.of("a1", "HashTask", 2, 4, "h1"));
    registry.register(AgentRegistration.of("a2", "HashTask", 2, 4, "h2"));
    registry.heartbeat("a1", 2, 2);
    registry.heartbeat("a2", 4, 0);

    Map<String, AgentAvailability> avail = registry.availability();
    AgentAvailability hashAvail = avail.get("HashTask");
    assertNotNull(hashAvail);
    assertEquals(2, hashAvail.getTotalAgents());
    assertEquals(8, hashAvail.getTotalSlots());
    assertEquals(6, hashAvail.getFreeSlots());
    assertEquals(2, hashAvail.getBusySlots());
  }

  @Test
  void unregisterRemovesAgent() {
    AgentRegistry registry = new AgentRegistry(30);
    registry.register(AgentRegistration.of("a1", "HashTask", 2, 4, "h1"));
    registry.unregister("a1");
    assertTrue(registry.liveAgents().isEmpty());
  }

  @Test
  void expiredAgentIsEvicted() throws InterruptedException {
    AgentRegistry registry = new AgentRegistry(1); // expire after 1s
    registry.register(AgentRegistration.of("a1", "HashTask", 2, 4, "h1"));
    Thread.sleep(1500);
    assertTrue(registry.liveAgents().isEmpty(), "Expired agent should be evicted");
  }
}
