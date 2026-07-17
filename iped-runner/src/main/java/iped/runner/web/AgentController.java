package iped.runner.web;

import iped.runner.distributed.AgentInventoryService;
import iped.runner.distributed.AgentRecord;
import java.util.List;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller exposing the live agent inventory.
 *
 * <p>Returns an empty list when Kafka is not configured rather than 503, so the dashboard can
 * always render the agents panel (just empty in local-only mode).
 */
@RestController
@RequestMapping("/v2/agents")
public class AgentController {

  private final AgentInventoryService agentInventory;

  public AgentController(AgentInventoryService agentInventory) {
    this.agentInventory = agentInventory;
  }

  /**
   * Lists all known remote processing agents with computed health status.
   *
   * @return JSON object with {@code enabled} flag and {@code agents} array.
   */
  @GetMapping
  public ResponseEntity<Map<String, Object>> listAgents() {
    List<AgentRecord> agents = agentInventory.listAgents();
    return ResponseEntity.ok(
        Map.of(
            "enabled", agentInventory.isEnabled(),
            "total", agents.size(),
            "active", agents.stream().filter(a -> "active".equals(a.health())).count(),
            "stale", agents.stream().filter(a -> "stale".equals(a.health())).count(),
            "offline", agents.stream().filter(a -> "offline".equals(a.health())).count(),
            "agents", agents.stream().map(AgentController::toJson).toList()));
  }

  private static Map<String, Object> toJson(AgentRecord a) {
    return Map.of(
        "agentId", a.agentId(),
        "hostname", a.hostname() != null ? a.hostname() : "",
        "startedAt", a.startedAt() != null ? a.startedAt().toString() : "",
        "lastSeen", a.lastSeen().toString(),
        "version", a.version() != null ? a.version() : "",
        "activeTasks", a.activeTasks(),
        "maxTasks", a.maxTasks(),
        "health", a.health());
  }
}
