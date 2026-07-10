package iped.distributed.coordinator;

import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.AdminClientConfig;
import org.apache.kafka.clients.admin.DescribeClusterOptions;

import java.util.Properties;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Probes real connectivity to the Kafka broker cluster, used to turn the coordinator's
 * {@code /health} endpoint from a liveness check into a readiness check (PI-1-F3-S1).
 *
 * <p>Reuses the same fail-open {@link AdminClient} discipline already established by
 * {@link iped.distributed.metrics.ConsumerLagProvider}: a single long-lived
 * {@code AdminClient} is created once (avoiding a TCP handshake per {@code /health} call)
 * and {@link #probe()} never throws — any failure (timeout, broker down, unexpected error
 * such as a SASL auth failure) is caught and reported as a {@link ProbeResult#degraded}.
 *
 * <h2>Timeout discipline (NFR-E2)</h2>
 * <p>The probe applies an explicit 3-second timeout on both sides: the
 * {@code DescribeClusterOptions} sent to the broker, and the client-side
 * {@code Future.get(...)} wait. The client-side timeout is what actually bounds the call —
 * it guarantees the handler cannot hang even if a given {@code AdminClient} implementation
 * ignored {@code timeoutMs} internally.
 */
@Slf4j
public class BrokerHealthProbe implements AutoCloseable {

    /** Explicit probe timeout (NFR-E2) — never rely on the AdminClient's default timeout. */
    static final int PROBE_TIMEOUT_MS = 3000;

    private final AdminClient admin;

    public BrokerHealthProbe(String bootstrapServers) {
        Properties p = new Properties();
        p.put(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        this.admin = AdminClient.create(p);
    }

    /** Test-only constructor — injects a (possibly mocked) {@link AdminClient} directly. */
    BrokerHealthProbe(AdminClient admin) {
        this.admin = admin;
    }

    /**
     * Probes the broker cluster. Never throws — any failure (timeout, connection refused,
     * unexpected broker-side error such as a SASL auth failure) is logged and reported as a
     * {@link ProbeResult#degraded} result with a sanitized reason, so the caller can fail
     * open on the HTTP response instead of propagating a 500.
     */
    public ProbeResult probe() {
        try {
            admin.describeCluster(new DescribeClusterOptions().timeoutMs(PROBE_TIMEOUT_MS))
                    .clusterId()
                    .get(PROBE_TIMEOUT_MS, TimeUnit.MILLISECONDS);
            return ProbeResult.ok();
        } catch (TimeoutException te) {
            log.warn("Kafka broker health probe timed out after {}ms", PROBE_TIMEOUT_MS);
            return ProbeResult.degraded("Kafka broker did not respond within " + PROBE_TIMEOUT_MS + "ms");
        } catch (Exception ex) {
            // Sanitized on purpose: only the exception's simple class name is surfaced to
            // callers/HTTP payload — the raw message could echo connection strings, SASL
            // principals, or other broker-side detail. Full detail still goes to the log.
            log.warn("Kafka broker health probe failed: {}", ex.toString());
            return ProbeResult.degraded("Kafka broker probe failed: " + ex.getClass().getSimpleName());
        }
    }

    @Override
    public void close() {
        try {
            admin.close();
        } catch (Exception ignored) {
            // best-effort — coordinator is shutting down anyway
        }
    }

    /** Outcome of a single broker connectivity probe. */
    public record ProbeResult(boolean reachable, String reason) {
        public static ProbeResult ok() {
            return new ProbeResult(true, null);
        }

        public static ProbeResult degraded(String reason) {
            return new ProbeResult(false, reason);
        }
    }
}
