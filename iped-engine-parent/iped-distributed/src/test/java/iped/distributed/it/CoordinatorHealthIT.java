package iped.distributed.it;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import iped.distributed.config.DistributedConfig;
import iped.distributed.coordinator.CoordinatorServer;
import iped.utils.UTF8Properties;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Path;
import java.time.Duration;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.kafka.KafkaContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * PI-1-F3-S2 — chaos/integration test for {@code GET /health} ({@link
 * CoordinatorServer.ApiServlet#handleHealth}) against a real Testcontainers Kafka broker, including
 * a real broker outage.
 *
 * <p>Unlike {@link iped.distributed.coordinator.BrokerHealthProbeTest} (unit tests with a mocked
 * {@code AdminClient}), this test drives the whole coordinator over real HTTP against a real broker
 * and then a real broker outage (container stop), so it also verifies the NFR-E2 wall-clock budget
 * (&lt;5s, since the internal probe itself times out at 3s) end to end — something a
 * mocked-AdminClient unit test cannot exercise.
 *
 * <p>Each test method gets its own (non-static) {@link KafkaContainer} instance so that stopping
 * the broker in the degraded-path test can never leak a stopped broker into any other test —
 * Testcontainers' JUnit 5 extension starts/stops the instance-level container around each test
 * method automatically, so no container/thread is left orphaned.
 */
@Tag("integration")
@Testcontainers(disabledWithoutDocker = true)
class CoordinatorHealthIT {

  private static final DockerImageName KAFKA_IMAGE = DockerImageName.parse("apache/kafka:3.8.1");

  @Container private final KafkaContainer kafka = new KafkaContainer(KAFKA_IMAGE);

  // ── AC3 (control/regression case): healthy broker → HTTP 200, brokerReachable:true ──

  @Test
  @Timeout(120)
  void givenHealthyBroker_whenHealthCalled_thenReturns200AndReachableTrue(
      @org.junit.jupiter.api.io.TempDir Path stateDir) throws Exception {
    int port = freePort();
    CoordinatorServer server = startCoordinator(kafka.getBootstrapServers(), stateDir, port);
    try {
      HttpResponse<String> resp = callHealth(port);

      assertEquals(200, resp.statusCode(), "healthy broker must yield HTTP 200: " + resp.body());
      assertTrue(
          resp.body().contains("\"status\":\"ok\""),
          "healthy broker must report status:ok, body=" + resp.body());
      assertTrue(
          resp.body().contains("\"brokerReachable\":true"),
          "healthy broker must report brokerReachable:true, body=" + resp.body());
    } finally {
      server.stop();
    }
  }

  // ── AC1 + AC2: broker stopped → HTTP 503, status:degraded, brokerReachable:false,
  //    and the round trip stays within the NFR-E2 wall-clock budget (<5000ms) ──────────

  @Test
  @Timeout(120)
  void givenBrokerStopped_whenHealthCalled_thenReturns503DegradedWithinBudget(
      @org.junit.jupiter.api.io.TempDir Path stateDir) throws Exception {
    int port = freePort();
    CoordinatorServer server = startCoordinator(kafka.getBootstrapServers(), stateDir, port);
    try {
      // Confirm the happy path once more on this coordinator instance before the outage,
      // so the 503 we assert below is unambiguously caused by the broker going down.
      HttpResponse<String> before = callHealth(port);
      assertEquals(200, before.statusCode(), "must be healthy before the simulated outage");

      kafka.stop();

      long startNanos = System.nanoTime();
      HttpResponse<String> after = callHealth(port);
      long elapsedMs = Duration.ofNanos(System.nanoTime() - startNanos).toMillis();

      assertEquals(503, after.statusCode(), "broker outage must yield HTTP 503: " + after.body());
      assertTrue(
          after.body().contains("\"status\":\"degraded\""),
          "broker outage must report status:degraded, body=" + after.body());
      assertTrue(
          after.body().contains("\"brokerReachable\":false"),
          "broker outage must report brokerReachable:false, body=" + after.body());
      assertFalse(
          elapsedMs >= 5000,
          "NFR-E2: /health must respond within 5000ms even during a broker outage "
              + "(internal probe times out at 3000ms) — took "
              + elapsedMs
              + "ms");
    } finally {
      server.stop();
    }
  }

  // ── Helpers ──────────────────────────────────────────────────────────────────────

  /** Starts a real {@link CoordinatorServer} bound to the given local port. */
  private static CoordinatorServer startCoordinator(
      String bootstrapServers, Path stateDir, int port) throws Exception {
    UTF8Properties props = new UTF8Properties();
    props.setProperty("kafkaBootstrapServers", bootstrapServers);
    props.setProperty("coordinatorPort", String.valueOf(port));
    props.setProperty("coordinatorStateDir", stateDir.toString());

    DistributedConfig cfg = new DistributedConfig();
    cfg.processProperties(props);

    CoordinatorServer server = new CoordinatorServer(cfg);
    server.start();
    return server;
  }

  private static HttpResponse<String> callHealth(int port)
      throws IOException, InterruptedException {
    HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
    HttpRequest request =
        HttpRequest.newBuilder()
            .uri(URI.create("http://localhost:" + port + "/api/v1/health"))
            .timeout(Duration.ofSeconds(10))
            .GET()
            .build();
    return client.send(request, HttpResponse.BodyHandlers.ofString());
  }

  /** Finds a free local TCP port to bind the coordinator's Jetty server to. */
  private static int freePort() throws IOException {
    try (ServerSocket socket = new ServerSocket(0)) {
      return socket.getLocalPort();
    }
  }
}
