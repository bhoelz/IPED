package iped.distributed.coordinator;

import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.DescribeClusterResult;
import org.apache.kafka.common.KafkaFuture;
import org.apache.kafka.common.errors.SaslAuthenticationException;
import org.junit.jupiter.api.Test;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * PI-1-F3-S1 — unit tests for {@link BrokerHealthProbe}, the readiness probe used by
 * {@code CoordinatorServer.handleHealth}.
 *
 * <p>Covers the 3 branches required by the story's DoD, with a mocked {@link AdminClient}:
 * <ol>
 *   <li>broker reachable → {@code ok}</li>
 *   <li>broker probe times out → {@code degraded}</li>
 *   <li>broker probe throws an unexpected (non-timeout) exception → {@code degraded},
 *       never propagated</li>
 * </ol>
 */
class BrokerHealthProbeTest {

    // ── Given the broker is accessible, when probed, then the result is reachable ──────

    @Test
    void givenBrokerAccessible_whenProbed_thenResultIsReachable() throws Exception {
        AdminClient admin = mock(AdminClient.class);
        DescribeClusterResult result = mock(DescribeClusterResult.class);
        @SuppressWarnings("unchecked")
        KafkaFuture<String> clusterIdFuture = mock(KafkaFuture.class);

        when(admin.describeCluster(any())).thenReturn(result);
        when(result.clusterId()).thenReturn(clusterIdFuture);
        when(clusterIdFuture.get(anyLong(), any(TimeUnit.class))).thenReturn("test-cluster-id");

        BrokerHealthProbe probe = newProbe(admin);
        BrokerHealthProbe.ProbeResult outcome = probe.probe();

        assertTrue(outcome.reachable());
        assertNull(outcome.reason());
    }

    // ── Given the broker is unreachable (timeout), when probed, then degraded ──────────

    @Test
    void givenProbeTimesOut_whenProbed_thenResultIsDegradedWithReason() throws Exception {
        AdminClient admin = mock(AdminClient.class);
        DescribeClusterResult result = mock(DescribeClusterResult.class);
        @SuppressWarnings("unchecked")
        KafkaFuture<String> clusterIdFuture = mock(KafkaFuture.class);

        when(admin.describeCluster(any())).thenReturn(result);
        when(result.clusterId()).thenReturn(clusterIdFuture);
        when(clusterIdFuture.get(anyLong(), any(TimeUnit.class))).thenThrow(new TimeoutException("simulated timeout"));

        BrokerHealthProbe probe = newProbe(admin);
        BrokerHealthProbe.ProbeResult outcome = probe.probe();

        assertFalse(outcome.reachable());
        assertNotNull(outcome.reason());
        assertTrue(outcome.reason().contains("3000ms"),
                "timeout reason should mention the explicit 3s probe timeout, was: " + outcome.reason());
    }

    // ── Given the probe throws an unexpected (non-timeout) exception, then degraded,
    //    never propagated, with a sanitized reason ─────────────────────────────────────

    @Test
    void givenUnexpectedException_whenProbed_thenResultIsDegradedAndNotPropagated() throws Exception {
        AdminClient admin = mock(AdminClient.class);
        DescribeClusterResult result = mock(DescribeClusterResult.class);
        @SuppressWarnings("unchecked")
        KafkaFuture<String> clusterIdFuture = mock(KafkaFuture.class);

        when(admin.describeCluster(any())).thenReturn(result);
        when(result.clusterId()).thenReturn(clusterIdFuture);
        SaslAuthenticationException authError =
                new SaslAuthenticationException("secret=super-sensitive-credential");
        when(clusterIdFuture.get(anyLong(), any(TimeUnit.class)))
                .thenThrow(new ExecutionException(authError));

        BrokerHealthProbe probe = newProbe(admin);

        BrokerHealthProbe.ProbeResult outcome = assertDoesNotThrow(probe::probe,
                "probe() must never propagate — fail-open per PI-1-F3-S1 AC 4");

        assertFalse(outcome.reachable());
        assertNotNull(outcome.reason());
        // Sanitized: only the exception's simple class name is surfaced, never the raw
        // (potentially sensitive) exception message.
        assertFalse(outcome.reason().contains("super-sensitive-credential"),
                "reason must not leak the raw exception message");
        assertTrue(outcome.reason().contains("ExecutionException"));
    }

    // ── Helpers ──────────────────────────────────────────────────────────────────────

    /** Same-package test constructor — injects the mocked AdminClient directly. */
    private static BrokerHealthProbe newProbe(AdminClient admin) {
        return new BrokerHealthProbe(admin);
    }
}
