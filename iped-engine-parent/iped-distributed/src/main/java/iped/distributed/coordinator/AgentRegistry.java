package iped.distributed.coordinator;


import lombok.extern.slf4j.Slf4j;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
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
@Slf4j
public class AgentRegistry {


    private final int expirySeconds;
    private final Map<String, AgentRegistration> agents = new ConcurrentHashMap<>();

    public AgentRegistry(int expirySeconds) {
        this.expirySeconds = expirySeconds;
    }

    // -----------------------------------------------------------------------

    public void register(AgentRegistration reg) {
        reg.setLastHeartbeat(Instant.now());
        agents.put(reg.getAgentId(), reg);
        log.info("Agent registered: id={}, type={}, stage={}, host={}",
                reg.getAgentId(), reg.getTaskType(), reg.getStageNumber(), reg.getHostname());
    }

    public void heartbeat(String agentId, int freeSlots, int currentLoad) {
        AgentRegistration reg = agents.get(agentId);
        if (reg != null) {
            reg.setLastHeartbeat(Instant.now());
            reg.setFreeSlots(freeSlots);
            reg.setCurrentLoad(currentLoad);
        } else {
            log.warn("Heartbeat from unknown agent '{}'", agentId);
        }
    }

    public void unregister(String agentId) {
        AgentRegistration removed = agents.remove(agentId);
        if (removed != null) {
            log.info("Agent unregistered: id={}, type={}", agentId, removed.getTaskType());
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
                log.warn("Agent '{}' (type={}) expired — last heartbeat was {}",
                        entry.getKey(),
                        entry.getValue().getTaskType(),
                        entry.getValue().getLastHeartbeat());
            }
            return expired;
        });
    }
}
