package iped.distributed.coordinator;

import iped.distributed.resource.PressureLevel;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;

/**
 * In-memory registry of active Task Agents.
 *
 * <p>Agents register on startup and send periodic heartbeats. Agents whose last heartbeat is older
 * than {@link #expirySeconds} are automatically evicted and considered dead.
 *
 * <p>This registry is held by the {@link CoordinatorServer} and is the source of truth for agent
 * availability queries.
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
    log.info(
        "Agent registered: id={}, type={}, stage={}, host={}",
        reg.getAgentId(),
        reg.getTaskType(),
        reg.getStageNumber(),
        reg.getHostname());
  }

  public void heartbeat(String agentId, int freeSlots, int currentLoad) {
    heartbeat(agentId, freeSlots, currentLoad, PressureLevel.NONE, 0.0);
  }

  /** Heartbeat carrying the agent's reported resource backpressure. */
  public void heartbeat(
      String agentId,
      int freeSlots,
      int currentLoad,
      PressureLevel pressureLevel,
      double pressureRatio) {
    AgentRegistration reg = agents.get(agentId);
    if (reg != null) {
      reg.setLastHeartbeat(Instant.now());
      reg.setFreeSlots(freeSlots);
      reg.setCurrentLoad(currentLoad);
      PressureLevel prev = reg.getPressureLevel();
      reg.setPressureLevel(pressureLevel != null ? pressureLevel : PressureLevel.NONE);
      reg.setPressureRatio(pressureRatio);
      if (prev != reg.getPressureLevel() && reg.getPressureLevel() == PressureLevel.HARD) {
        log.warn(
            "Agent '{}' (type={}) entered HARD backpressure — withholding new work",
            agentId,
            reg.getTaskType());
      }
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
   * Returns aggregated availability per task type, including only live agents. Map key = taskType.
   */
  public Map<String, AgentAvailability> availability() {
    evictExpired();
    Map<String, AgentAvailability> result = new LinkedHashMap<>();

    agents.values().stream()
        .collect(Collectors.groupingBy(AgentRegistration::getTaskType))
        .forEach(
            (taskType, list) -> {
              int totalSlots = list.stream().mapToInt(AgentRegistration::getMaxParallelItems).sum();
              int freeSlots = list.stream().mapToInt(AgentRegistration::getFreeSlots).sum();
              int stage = list.get(0).getStageNumber();
              result.put(
                  taskType,
                  AgentAvailability.of(taskType, stage, list.size(), totalSlots, freeSlots));
            });

    return result;
  }

  /** Returns the registration for a specific agent ID, or {@code null} if not found. */
  public AgentRegistration getAgent(String agentId) {
    evictExpired();
    return agents.get(agentId);
  }

  /** Returns live agents for a specific task type. */
  public List<AgentRegistration> agentsForTaskType(String taskType) {
    evictExpired();
    return agents.values().stream()
        .filter(r -> taskType.equals(r.getTaskType()))
        .collect(Collectors.toList());
  }

  /**
   * Free slots available for scheduling new work of the given task type, <b>excluding agents under
   * HARD backpressure</b> (those report "send me no new work"). This is the value the coordinator
   * feeds to the {@code CaseScheduler} as available capacity, so resource-pressured agents
   * naturally stop attracting work without losing their in-flight items.
   */
  public int schedulableFreeSlots(String taskType) {
    evictExpired();
    return agents.values().stream()
        .filter(r -> taskType.equals(r.getTaskType()))
        .filter(AgentRegistration::isSchedulable)
        .mapToInt(AgentRegistration::getFreeSlots)
        .sum();
  }

  /** Number of live agents of a task type currently under HARD backpressure. */
  public long pressuredAgentCount(String taskType) {
    evictExpired();
    return agents.values().stream()
        .filter(r -> taskType.equals(r.getTaskType()))
        .filter(r -> !r.isSchedulable())
        .count();
  }

  // -----------------------------------------------------------------------

  private void evictExpired() {
    Instant threshold = Instant.now().minusSeconds(expirySeconds);
    agents
        .entrySet()
        .removeIf(
            entry -> {
              boolean expired = entry.getValue().getLastHeartbeat().isBefore(threshold);
              if (expired) {
                log.warn(
                    "Agent '{}' (type={}) expired — last heartbeat was {}",
                    entry.getKey(),
                    entry.getValue().getTaskType(),
                    entry.getValue().getLastHeartbeat());
              }
              return expired;
            });
  }
}
