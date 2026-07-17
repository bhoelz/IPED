package iped.distributed.coordinator;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;

/**
 * HTTP client for the {@link CoordinatorServer} REST API.
 *
 * <p>Used by Task Agents to:
 *
 * <ul>
 *   <li>Register themselves on startup
 *   <li>Send periodic heartbeats
 *   <li>Query the pipeline stage for their task type
 *   <li>Unregister on shutdown
 * </ul>
 *
 * <p>Uses only {@code java.net.HttpURLConnection} — no external HTTP client dependency.
 */
@Slf4j
public class CoordinatorClient {

  private final String baseUrl;
  private final ObjectMapper mapper;

  public CoordinatorClient(String coordinatorServerUrl) {
    this.baseUrl = coordinatorServerUrl.replaceAll("/$", "");
    this.mapper = new ObjectMapper().registerModule(new JavaTimeModule());
  }

  // -----------------------------------------------------------------------

  public void register(AgentRegistration reg) {
    try {
      String body = mapper.writeValueAsString(reg);
      post("/api/v1/agents/register", body);
      log.info(
          "Registered with coordinator as agent '{}' (type={})",
          reg.getAgentId(),
          reg.getTaskType());
    } catch (Exception e) {
      log.warn("Could not register with coordinator: {}", e.getMessage());
    }
  }

  /**
   * Registers with the coordinator, retrying on failure with exponential backoff.
   *
   * @param reg agent registration descriptor
   * @param maxAttempts maximum number of attempts (must be ≥ 1)
   * @param baseDelayMs base delay between retries; actual delay = {@code baseDelayMs * 2^attempt}
   * @throws RuntimeException when all attempts are exhausted
   */
  public void registerWithRetry(AgentRegistration reg, int maxAttempts, long baseDelayMs) {
    Exception lastEx = null;
    for (int attempt = 0; attempt < maxAttempts; attempt++) {
      try {
        String body = mapper.writeValueAsString(reg);
        post("/api/v1/agents/register", body);
        log.info(
            "Registered with coordinator as agent '{}' (type={}) after {} attempt(s)",
            reg.getAgentId(),
            reg.getTaskType(),
            attempt + 1);
        return;
      } catch (Exception e) {
        lastEx = e;
        long delay = baseDelayMs * (1L << Math.min(attempt, 10));
        log.warn(
            "Registration attempt {}/{} failed: {} — retrying in {}ms",
            attempt + 1,
            maxAttempts,
            e.getMessage(),
            delay);
        if (attempt + 1 < maxAttempts) {
          try {
            Thread.sleep(delay);
          } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Registration interrupted", ie);
          }
        }
      }
    }
    throw new RuntimeException(
        "Could not register with coordinator after " + maxAttempts + " attempt(s)", lastEx);
  }

  public HeartbeatResponse heartbeat(String agentId, int freeSlots, int currentLoad) {
    return heartbeat(
        agentId, freeSlots, currentLoad, iped.distributed.resource.PressureLevel.NONE, 0.0);
  }

  /**
   * Heartbeat carrying the agent's reported resource backpressure.
   *
   * @return the coordinator's response containing the topic list for this agent, or an empty
   *     response when the coordinator is unreachable
   */
  public HeartbeatResponse heartbeat(
      String agentId,
      int freeSlots,
      int currentLoad,
      iped.distributed.resource.PressureLevel pressureLevel,
      double pressureRatio) {
    try {
      String body =
          String.format(
              java.util.Locale.ROOT,
              "{\"agentId\":\"%s\",\"freeSlots\":%d,\"currentLoad\":%d,"
                  + "\"pressureLevel\":\"%s\",\"pressureRatio\":%.4f}",
              agentId,
              freeSlots,
              currentLoad,
              pressureLevel != null ? pressureLevel : iped.distributed.resource.PressureLevel.NONE,
              pressureRatio);
      String responseJson = postWithResponse("/api/v1/agents/" + agentId + "/heartbeat", body);
      if (responseJson != null && !responseJson.isBlank()) {
        return mapper.readValue(responseJson, HeartbeatResponse.class);
      }
    } catch (Exception e) {
      log.debug("Heartbeat failed: {}", e.getMessage());
    }
    return new HeartbeatResponse();
  }

  public void unregister(String agentId) {
    try {
      delete("/api/v1/agents/" + agentId);
    } catch (Exception e) {
      log.warn("Could not unregister agent '{}': {}", agentId, e.getMessage());
    }
  }

  /**
   * Returns the pipeline stage number for the given task type in the given case. Task Agents call
   * this during startup to know which Kafka topics to use.
   */
  public int getStageForTask(String caseId, String taskType) {
    try {
      String response = get("/api/v1/pipeline/" + caseId);
      @SuppressWarnings("unchecked")
      Map<String, Integer> map = mapper.readValue(response, Map.class);
      Integer stage = map.get(taskType);
      if (stage == null)
        throw new IllegalArgumentException(
            "Task '" + taskType + "' not found in pipeline for case '" + caseId + "'");
      return stage;
    } catch (Exception e) {
      throw new RuntimeException(
          "Could not get stage for task '" + taskType + "' in case '" + caseId + "'", e);
    }
  }

  @SuppressWarnings("unchecked")
  public Map<String, AgentAvailability> getAvailability() throws Exception {
    String json = get("/api/v1/agents/availability");
    return mapper.readValue(json, Map.class);
  }

  // -----------------------------------------------------------------------
  // HTTP helpers
  // -----------------------------------------------------------------------

  private String get(String path) throws IOException {
    URL url = new URL(baseUrl + path);
    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
    conn.setRequestMethod("GET");
    conn.setConnectTimeout(5000);
    conn.setReadTimeout(10000);
    conn.setRequestProperty("Accept", "application/json");
    int code = conn.getResponseCode();
    if (code >= 400) throw new IOException("HTTP " + code + " for GET " + path);
    return new String(conn.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
  }

  @SuppressWarnings("UnusedReturnValue")
  private void post(String path, String json) throws IOException {
    postWithResponse(path, json);
  }

  private String postWithResponse(String path, String json) throws IOException {
    URL url = new URL(baseUrl + path);
    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
    conn.setRequestMethod("POST");
    conn.setDoOutput(true);
    conn.setConnectTimeout(5000);
    conn.setReadTimeout(10000);
    conn.setRequestProperty("Content-Type", "application/json");
    try (OutputStream os = conn.getOutputStream()) {
      os.write(json.getBytes(StandardCharsets.UTF_8));
    }
    int code = conn.getResponseCode();
    if (code >= 400) throw new IOException("HTTP " + code + " for POST " + path);
    return new String(conn.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
  }

  private void delete(String path) throws IOException {
    URL url = new URL(baseUrl + path);
    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
    conn.setRequestMethod("DELETE");
    conn.setConnectTimeout(5000);
    conn.setReadTimeout(5000);
    conn.getResponseCode(); // consume
  }
}
