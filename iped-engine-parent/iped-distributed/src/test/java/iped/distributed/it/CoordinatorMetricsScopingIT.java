package iped.distributed.it;

import com.fasterxml.jackson.databind.ObjectMapper;
import iped.distributed.config.DistributedConfig;
import iped.distributed.coordinator.CaseLifecycleManager;
import iped.distributed.coordinator.CoordinatorServer;
import iped.utils.UTF8Properties;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.kafka.KafkaContainer;
import org.testcontainers.utility.DockerImageName;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.ServerSocket;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

/**
 * Regression coverage (CRITICAL code-review finding, PI-1-F2-S1) for
 * {@code CoordinatorServer.MetricsServlet}: the {@code GET /metrics} scrape must scope its
 * DLQ-count / stall-detection gauges to {@code RUNNING} cases only — the same case-scoping
 * {@code /health} already applies (see {@code handleHealth}) — never to
 * COMPLETED/FAILED/PAUSED cases.
 *
 * <p>Before the fix, {@code MetricsServlet} iterated {@code lifecycle.allCases()} with no
 * state filter, so a completed case's DLQ-count and stall gauges kept being computed and
 * published forever (misleadingly showing {@code case_stalled=1} on a case with no active
 * work), and every scrape paid the per-case Kafka round-trip cost across the coordinator's
 * entire case history rather than just its active cases.
 *
 * <p>Drives the whole coordinator over real HTTP against a real Testcontainers Kafka broker,
 * exactly like {@link CoordinatorHealthIT}, since the servlet under test wires together
 * {@code CaseLifecycleManager}, {@code DlqManager} and {@code CaseCompletionMonitor} — all
 * constructed with real Kafka clients inside {@code CoordinatorServer#start()}.
 */
@Tag("integration")
@Testcontainers(disabledWithoutDocker = true)
class CoordinatorMetricsScopingIT {

    private static final DockerImageName KAFKA_IMAGE =
            DockerImageName.parse("apache/kafka:3.8.1");

    @Container
    private final KafkaContainer kafka = new KafkaContainer(KAFKA_IMAGE);

    // ── COMPLETED/PAUSED cases must not contribute dlq_count/case_stalled samples ──────

    @Test
    @Timeout(180)
    void givenMixedCaseStates_whenMetricsScraped_thenOnlyRunningCaseHasDlqAndStallGauges(
            @org.junit.jupiter.api.io.TempDir Path stateDir) throws Exception {
        int port = freePort();
        CoordinatorServer server = startCoordinator(kafka.getBootstrapServers(), stateDir, port);
        try {
            startCase(port, "running-case");
            startCase(port, "completed-case");
            startCase(port, "paused-case");

            // Move the other two cases out of RUNNING state.
            pauseCase(port, "paused-case");
            completeCase(server, "completed-case");

            String scrape = callMetrics(port);

            // The RUNNING case is allowed to appear (gauges are best-effort/degrade to
            // 0/false, so we don't assert its presence — only that the non-RUNNING cases
            // are excluded).
            assertFalse(scrape.contains("iped_distributed_dlq_count{case_id=\"completed-case\""),
                    "COMPLETED case must be excluded from dlq_count gauge, scrape=\n" + scrape);
            assertFalse(scrape.contains("iped_distributed_dlq_count{case_id=\"paused-case\""),
                    "PAUSED case must be excluded from dlq_count gauge, scrape=\n" + scrape);
            assertFalse(scrape.contains("iped_distributed_case_stalled{case_id=\"completed-case\""),
                    "COMPLETED case must be excluded from case_stalled gauge, scrape=\n" + scrape);
            assertFalse(scrape.contains("iped_distributed_case_stalled{case_id=\"paused-case\""),
                    "PAUSED case must be excluded from case_stalled gauge, scrape=\n" + scrape);
        } finally {
            server.stop();
        }
    }

    // ── Helpers ──────────────────────────────────────────────────────────────────────

    private static CoordinatorServer startCoordinator(String bootstrapServers, Path stateDir, int port)
            throws Exception {
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

    private static void startCase(int port, String caseId) throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        Map<String, Object> body = Map.of(
                "caseId", caseId,
                "orderedTaskNames", List.of("HashTask"),
                "topicPartitions", 1,
                "replicationFactor", 1);
        HttpResponse<String> resp = post(port, "/api/v1/cases/start", mapper.writeValueAsString(body));
        assertEquals(200, resp.statusCode(), "case start must succeed: " + resp.body());
    }

    private static void pauseCase(int port, String caseId) throws Exception {
        HttpResponse<String> resp = post(port, "/api/v1/cases/" + caseId + "/pause", "{}");
        assertEquals(200, resp.statusCode(), "case pause must succeed: " + resp.body());
    }

    /**
     * There is no public REST endpoint to force a case to COMPLETED (it normally happens
     * automatically once every discovered item finishes, driven by
     * {@code CaseCompletionMonitor}). We reach into the coordinator's private
     * {@code CaseLifecycleManager} via reflection — exactly the state transition
     * {@code CaseCompletionMonitor} itself triggers in production — so the case stays
     * present in {@code lifecycle.allCases()} (unlike {@code deleteCase}, which would just
     * remove it and trivially "pass" without exercising the state-filter fix).
     */
    private static void completeCase(CoordinatorServer server, String caseId) throws Exception {
        java.lang.reflect.Field lifecycleField = CoordinatorServer.class.getDeclaredField("lifecycle");
        lifecycleField.setAccessible(true);
        CaseLifecycleManager lifecycle = (CaseLifecycleManager) lifecycleField.get(server);
        lifecycle.completeCase(caseId);
    }

    private static HttpResponse<String> post(int port, String path, String body) throws Exception {
        HttpClient client = httpClient();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + port + path))
                .timeout(Duration.ofSeconds(10))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();
        return client.send(request, HttpResponse.BodyHandlers.ofString());
    }

    private static String callMetrics(int port) throws Exception {
        HttpClient client = httpClient();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + port + "/metrics"))
                .timeout(Duration.ofSeconds(10))
                .GET()
                .build();
        return client.send(request, HttpResponse.BodyHandlers.ofString()).body();
    }

    private static HttpClient httpClient() {
        return HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
    }

    private static int freePort() throws Exception {
        try (ServerSocket socket = new ServerSocket(0)) {
            return socket.getLocalPort();
        }
    }
}
