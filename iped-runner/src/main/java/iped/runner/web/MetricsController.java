package iped.runner.web;

import iped.runner.distributed.DistributedStatusService;
import iped.runner.execution.ExecutionService;
import iped.runner.execution.RunQueueService;
import java.lang.management.ManagementFactory;
import java.time.Instant;
import java.util.*;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Lightweight JSON metrics endpoint for the runner observability stack.
 *
 * <p>{@code GET /metrics} returns a flat JSON object suitable for scraping by Prometheus (via a
 * custom exporter), Grafana, or just {@code curl | jq}.
 *
 * <h2>Metric groups</h2>
 *
 * <ul>
 *   <li>{@code runner.*} — queue depth, active runs, slot availability.
 *   <li>{@code distributed.*} — active distributed cases observed on Kafka.
 *   <li>{@code jvm.*} — heap usage, uptime (informational).
 * </ul>
 */
@RestController
@RequiredArgsConstructor
public class MetricsController {

  private final ExecutionService executionService;
  private final RunQueueService runQueueService;

  /** May be absent if Kafka support is not on the classpath/config. */
  private final Optional<DistributedStatusService> distributedStatus;

  @Value("${runner.max-concurrent:2}")
  private int maxConcurrent;

  @GetMapping("/metrics")
  public ResponseEntity<Map<String, Object>> metrics() {
    var m = new LinkedHashMap<String, Object>();

    // Runner queue / concurrency
    int activeRuns = executionService.snapshots().size();
    int pendingRuns = runQueueService.pending().size();
    int slotsUsed = activeRuns;
    int slotsAvail = Math.max(0, maxConcurrent - slotsUsed);

    m.put("runner.active_runs", activeRuns);
    m.put("runner.queued_runs", pendingRuns);
    m.put("runner.slots_total", maxConcurrent);
    m.put("runner.slots_available", slotsAvail);
    m.put("runner.profiles_available", runQueueService.availableProfiles().size());

    // Distributed / Kafka
    if (distributedStatus.isPresent()) {
      var ds = distributedStatus.get();
      int distActive = ds.snapshots().size();
      m.put("distributed.active_cases", distActive);
      m.put("distributed.kafka_available", true);
    } else {
      m.put("distributed.kafka_available", false);
    }

    // JVM
    var heap = ManagementFactory.getMemoryMXBean().getHeapMemoryUsage();
    m.put("jvm.heap_used_bytes", heap.getUsed());
    m.put("jvm.heap_max_bytes", heap.getMax());
    m.put("jvm.uptime_ms", ManagementFactory.getRuntimeMXBean().getUptime());

    m.put("ts", Instant.now().toString());
    return ResponseEntity.ok(m);
  }
}
