package iped.distributed.coordinator;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * In-memory registry of active Task Agents.
 *
 * <p>Agents register on startup and send periodic heartbeats. Agents whose
 * last heartbeat is older than {@link #expirySeconds} are automatically
 * evicted and considered dead.
 *
 * <p>This registry is held by the {@link CoordinatorServer} and is the
 * source of truth for agent availability queries.
 */
public class AgentRegistry {

    private static final Logger LOGGER = LoggerFactory.getLogger(AgentRegistry.class);

    private final int expirySeconds;
    private final Map<String, AgentRegistration> agents = new ConcurrentHashMap<>();

    public AgentRegistry(int expirySeconds) {
        this.expirySeconds = expirySeconds;
    }

    // -----------------------------------------------------------------------

    public void register(AgentRegistration reg) {
        reg.setLastHeartbeat(Instant.now());
        agents.put(reg.getAgentId(), reg);
        LOGGER.info("Agent registered: id={}, type={}, stage={}, host={}",
                reg.getAgentId(), reg.getTaskType(), reg.getStageNumber(), reg.getHostname());
    }

    public void heartbeat(String agentId, int freeSlots, int currentLoad) {
        AgentRegistration reg = agents.get(agentId);
        if (reg != null) {
            reg.setLastHeartbeat(Instant.now());
            reg.setFreeSlots(freeSlots);
            reg.setCurrentLoad(currentLoad);
        } else {
            LOGGER.warn("Heartbeat from unknown agent '{}'", agentId);
        }
    }

    public void unregister(String agentId) {
        AgentRegistration removed = agents.remove(agentId);
        if (removed != null) {
            LOGGER.info("Agent unregistered: id={}, type={}", agentId, removed.getTaskType());
        }
    }

    /** Returns a snapshot of all currently live agents. */
    public List<AgentRegistration> liveAgents() {
        evictExpired();
        return new ArrayList<>(agents.values());
    }

    /**
     * Returns aggregated availability per task type, including only live agents.
     * Map key = taskType.
     */
    public Map<String, AgentAvailability> availability() {
        evictExpired();
        Map<String, AgentAvailability> result = new LinkedHashMap<>();

        agents.values().stream()
              .collect(Collectors.groupingBy(AgentRegistration::getTaskType))
              .forEach((taskType, list) -> {
                  int totalSlots = list.stream().mapToInt(AgentRegistration::getMaxParallelItems).sum();
                  int freeSlots  = list.stream().mapToInt(AgentRegistration::getFreeSlots).sum();
                  int stage      = list.get(0).getStageNumber();
                  result.put(taskType,
                          AgentAvailability.of(taskType, stage, list.size(), totalSlots, freeSlots));
              });

        return result;
    }

    /** Returns live agents for a specific task type. */
    public List<AgentRegistration> agentsForTaskType(String taskType) {
        evictExpired();
        return agents.values().stream()
                     .filter(r -> taskType.equals(r.getTaskType()))
                     .collect(Collectors.toList());
    }

    // -----------------------------------------------------------------------

    private void evictExpired() {
        Instant threshold = Instant.now().minusSeconds(expirySeconds);
        agents.entrySet().removeIf(entry -> {
            boolean expired = entry.getValue().getLastHeartbeat().isBefore(threshold);
            if (expired) {
                LOGGER.warn("Agent '{}' (type={}) expired — last heartbeat was {}",
                        entry.getKey(),
                        entry.getValue().getTaskType(),
                        entry.getValue().getLastHeartbeat());
            }
            return expired;
        });
    }
}
