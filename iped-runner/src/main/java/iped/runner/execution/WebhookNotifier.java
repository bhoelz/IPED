package iped.runner.execution;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Fires a webhook HTTP POST whenever a run reaches a terminal state (COMPLETED, FAILED, ABORTED,
 * TIMED_OUT).
 *
 * <p>Configure with:
 *
 * <pre>
 * runner.webhook.url=https://your-server/hooks/iped-runner
 * runner.webhook.secret=optional-bearer-token
 * runner.webhook.timeout-seconds=10   # default
 * </pre>
 *
 * <p>When {@code runner.webhook.url} is blank the notifier is a no-op. Delivery failures are logged
 * at WARN level and do not affect the run's recorded state.
 */
@Component
@Slf4j
public class WebhookNotifier {

  @Value("${runner.webhook.url:}")
  private String webhookUrl;

  @Value("${runner.webhook.secret:}")
  private String webhookSecret;

  @Value("${runner.webhook.timeout-seconds:10}")
  private int timeoutSeconds;

  private final ObjectMapper mapper =
      new ObjectMapper()
          .registerModule(new JavaTimeModule())
          .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

  private final ExecutorService deliveryPool =
      Executors.newCachedThreadPool(
          r -> {
            Thread t = new Thread(r, "webhook-delivery");
            t.setDaemon(true);
            return t;
          });

  private final HttpClient httpClient =
      HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).executor(deliveryPool).build();

  /**
   * Fires the webhook asynchronously for a terminal run. Returns immediately; delivery happens on a
   * daemon thread.
   */
  public void notifyTerminal(RunSummary summary) {
    if (webhookUrl == null || webhookUrl.isBlank()) return;

    deliveryPool.submit(() -> deliver(summary));
  }

  private void deliver(RunSummary summary) {
    try {
      Map<String, Object> payload = new LinkedHashMap<>();
      payload.put("event", "run." + summary.status().name().toLowerCase());
      payload.put("runId", summary.id());
      payload.put("name", summary.name());
      payload.put("source", summary.source());
      payload.put("status", summary.status().name().toLowerCase());
      payload.put("exitCode", summary.exitCode());
      payload.put("startedAt", summary.startedAt() != null ? summary.startedAt().toString() : null);
      payload.put("endedAt", summary.endedAt() != null ? summary.endedAt().toString() : null);
      payload.put("itemsProcessed", summary.itemsProcessed());
      payload.put("itemsFound", summary.itemsFound());

      String body = mapper.writeValueAsString(payload);

      var reqBuilder =
          HttpRequest.newBuilder()
              .uri(URI.create(webhookUrl))
              .timeout(Duration.ofSeconds(timeoutSeconds))
              .header("Content-Type", "application/json")
              .POST(HttpRequest.BodyPublishers.ofString(body));

      if (webhookSecret != null && !webhookSecret.isBlank()) {
        reqBuilder.header("Authorization", "Bearer " + webhookSecret);
      }

      HttpResponse<String> resp =
          httpClient.send(reqBuilder.build(), HttpResponse.BodyHandlers.ofString());

      if (resp.statusCode() >= 200 && resp.statusCode() < 300) {
        log.debug("Webhook delivered for run {} (HTTP {})", summary.id(), resp.statusCode());
      } else {
        log.warn("Webhook delivery for run {} returned HTTP {}", summary.id(), resp.statusCode());
      }
    } catch (Exception e) {
      log.warn("Webhook delivery failed for run {}: {}", summary.id(), e.getMessage());
    }
  }
}
