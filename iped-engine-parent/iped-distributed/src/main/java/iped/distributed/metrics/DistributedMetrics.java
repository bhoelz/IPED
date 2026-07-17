package iped.distributed.metrics;

import iped.distributed.coordinator.AgentRegistration;
import iped.distributed.coordinator.AgentRegistry;
import iped.distributed.status.ItemStatusEvent;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.LongAdder;

/**
 * In-process metrics registry for the IPED distributed pipeline.
 *
 * <h2>Collected metrics</h2>
 *
 * <ul>
 *   <li><b>Counters</b> (driven by {@link ItemStatusEvent}s from the {@code iped.status} topic,
 *       consumed by the coordinator):
 *       <ul>
 *         <li>{@code iped_distributed_items_processed_total} — COMPLETED events
 *         <li>{@code iped_distributed_items_failed_total} — ERROR events
 *         <li>{@code iped_distributed_processing_duration_ms_total} — cumulative processing time
 *             from COMPLETED events; divide by processed count for average latency
 *       </ul>
 *   <li><b>Gauges</b> (live snapshot from {@link AgentRegistry} at scrape time):
 *       <ul>
 *         <li>{@code iped_distributed_agent_inflight} — items being processed per agent
 *         <li>{@code iped_distributed_agent_free_slots} — available parallelism per agent
 *       </ul>
 *   <li><b>Consumer lag</b> (optional; passed in from {@link ConsumerLagProvider} at scrape time;
 *       omitted when Kafka is unavailable):
 *       <ul>
 *         <li>{@code iped_distributed_consumer_lag} — messages behind end-of-partition
 *       </ul>
 * </ul>
 *
 * <h2>Thread safety</h2>
 *
 * <p>{@link #recordEvent} is safe to call from any thread. {@link #scrape} produces a consistent
 * snapshot: each individual counter is read atomically; there is no cross-counter transaction,
 * which is the standard trade-off for Prometheus-style instrumentation.
 *
 * <h2>Prometheus text format</h2>
 *
 * <p>{@link #scrape} returns a {@code text/plain; version=0.0.4} body. Feed it directly to a {@code
 * /metrics} endpoint or a Prometheus {@code textfile} collector.
 */
public class DistributedMetrics {

  // -------------------------------------------------------------------------
  // Key types
  // -------------------------------------------------------------------------

  /** Scopes a metric series to one (case, task-type, stage) combination. */
  record MetricKey(String caseId, String taskType, int stage) {}

  /**
   * Identifies one lag data-point: a single Kafka partition for a given consumer group. Produced by
   * {@link ConsumerLagProvider}; consumed by {@link #scrape}.
   */
  public record LagKey(String caseId, String consumerGroup, String topic, int partition) {}

  // -------------------------------------------------------------------------
  // Counters
  // -------------------------------------------------------------------------

  private final ConcurrentHashMap<MetricKey, LongAdder> processed = new ConcurrentHashMap<>();
  private final ConcurrentHashMap<MetricKey, LongAdder> failed = new ConcurrentHashMap<>();
  private final ConcurrentHashMap<MetricKey, LongAdder> durationMs = new ConcurrentHashMap<>();

  // -------------------------------------------------------------------------
  // Event ingestion
  // -------------------------------------------------------------------------

  /**
   * Records a single pipeline status event. Increments the appropriate counter. Events without a
   * {@code taskType} (e.g. DISCOVERED, CASE_COMPLETED) are skipped because they carry no per-stage
   * breakdown.
   *
   * <p>Safe to call from multiple threads concurrently.
   */
  public void recordEvent(ItemStatusEvent event) {
    if (event == null || event.getType() == null) return;
    String taskType = event.getTaskType();
    if (taskType == null || taskType.isBlank()) return;

    MetricKey key = new MetricKey(safe(event.getCaseId()), taskType, event.getPipelineStage());

    switch (event.getType()) {
      case COMPLETED -> {
        processed.computeIfAbsent(key, k -> new LongAdder()).increment();
        durationMs.computeIfAbsent(key, k -> new LongAdder()).add(event.getDurationMs());
      }
      case ERROR -> failed.computeIfAbsent(key, k -> new LongAdder()).increment();
      default -> {
        /* other event types not individually metered */
      }
    }
  }

  // -------------------------------------------------------------------------
  // Scrape
  // -------------------------------------------------------------------------

  /**
   * Produces a complete Prometheus text-format scrape body.
   *
   * @param registry live agent registry for in-flight / free-slot gauges
   * @param lagByKey consumer-lag snapshot from {@link ConsumerLagProvider}; pass {@code Map.of()}
   *     when Kafka is unavailable — the lag metric family is simply omitted
   * @return UTF-8 Prometheus text (content-type {@code text/plain; version=0.0.4})
   */
  public String scrape(AgentRegistry registry, Map<LagKey, Long> lagByKey) {
    return scrape(registry, lagByKey, Map.of(), Map.of());
  }

  /**
   * Produces a complete Prometheus text-format scrape body, including the optional DLQ-depth and
   * case-stall gauges.
   *
   * @param registry live agent registry for in-flight / free-slot gauges
   * @param lagByKey consumer-lag snapshot from {@link ConsumerLagProvider}; pass {@code Map.of()}
   *     when Kafka is unavailable
   * @param dlqCountByCase DLQ entry count per case, from {@link
   *     iped.distributed.kafka.DlqManager#count(String)}; pass {@code Map.of()} to omit the family
   * @param stalledByCase stall flag per case, from {@link
   *     iped.distributed.coordinator.CaseCompletionMonitor#isStalled(String, long)}; pass {@code
   *     Map.of()} to omit the family
   * @return UTF-8 Prometheus text (content-type {@code text/plain; version=0.0.4})
   */
  public String scrape(
      AgentRegistry registry,
      Map<LagKey, Long> lagByKey,
      Map<String, Long> dlqCountByCase,
      Map<String, Boolean> stalledByCase) {
    StringBuilder sb = new StringBuilder(4096);

    // ── Per-stage processing counters ──────────────────────────────────────
    writeCounterFamily(
        sb,
        processed,
        "iped_distributed_items_processed_total",
        "Items successfully processed per case/task-type/pipeline-stage.");
    writeCounterFamily(
        sb,
        failed,
        "iped_distributed_items_failed_total",
        "Items that encountered a processing error per case/task-type/stage.");
    writeCounterFamily(
        sb,
        durationMs,
        "iped_distributed_processing_duration_ms_total",
        "Cumulative processing time in milliseconds per case/task-type/stage; "
            + "divide by items_processed_total for average latency.");

    // ── Agent gauges (live) ────────────────────────────────────────────────
    List<AgentRegistration> agents = registry.liveAgents();
    if (!agents.isEmpty()) {
      appendHelp(
          sb,
          "iped_distributed_agent_inflight",
          "gauge",
          "Items currently being processed by each agent.");
      appendHelp(
          sb,
          "iped_distributed_agent_free_slots",
          "gauge",
          "Available item-processing slots per agent.");
      for (AgentRegistration a : agents) {
        String lbl = agentLabels(a);
        appendSample(sb, "iped_distributed_agent_inflight", lbl, a.getCurrentLoad());
        appendSample(sb, "iped_distributed_agent_free_slots", lbl, a.getFreeSlots());
      }
    }

    // ── Consumer lag (optional) ────────────────────────────────────────────
    if (!lagByKey.isEmpty()) {
      appendHelp(
          sb,
          "iped_distributed_consumer_lag",
          "gauge",
          "Kafka consumer lag in messages behind log-end-offset, per partition.");
      for (var e : lagByKey.entrySet()) {
        LagKey k = e.getKey();
        String lbl =
            String.format(
                "case_id=\"%s\",consumer_group=\"%s\",topic=\"%s\",partition=\"%d\"",
                escape(k.caseId()), escape(k.consumerGroup()), escape(k.topic()), k.partition());
        appendSample(sb, "iped_distributed_consumer_lag", lbl, e.getValue());
      }
    }

    // -- DLQ depth (optional) ------------------------------------------------
    if (!dlqCountByCase.isEmpty()) {
      appendHelp(
          sb,
          "iped_distributed_dlq_count",
          "gauge",
          "Unconsumed dead-letter-queue entries per case.");
      for (var e : dlqCountByCase.entrySet()) {
        String lbl = String.format("case_id=\"%s\"", escape(e.getKey()));
        appendSample(sb, "iped_distributed_dlq_count", lbl, e.getValue());
      }
    }

    // -- Case-stall detection (optional) -------------------------------------
    if (!stalledByCase.isEmpty()) {
      appendHelp(
          sb,
          "iped_distributed_case_stalled",
          "gauge",
          "1 when a case appears stalled (items discovered but not completed, "
              + "none in flight, no status events for the configured stall window); 0 otherwise.");
      for (var e : stalledByCase.entrySet()) {
        String lbl = String.format("case_id=\"%s\"", escape(e.getKey()));
        appendSample(sb, "iped_distributed_case_stalled", lbl, e.getValue() ? 1L : 0L);
      }
    }

    return sb.toString();
  }

  /**
   * Returns a human-readable counter snapshot for a specific case. Useful for log output and the
   * coordinator's case-status endpoint.
   */
  public Map<String, Long> countersForCase(String caseId) {
    Map<String, Long> out = new LinkedHashMap<>();
    processed.forEach(
        (k, v) -> {
          if (caseId.equals(k.caseId()))
            out.put("processed[" + k.taskType() + "][stage=" + k.stage() + "]", v.sum());
        });
    failed.forEach(
        (k, v) -> {
          if (caseId.equals(k.caseId()))
            out.put("failed[" + k.taskType() + "][stage=" + k.stage() + "]", v.sum());
        });
    return out;
  }

  // -------------------------------------------------------------------------
  // Internal helpers
  // -------------------------------------------------------------------------

  private static void writeCounterFamily(
      StringBuilder sb, ConcurrentHashMap<MetricKey, LongAdder> map, String name, String help) {
    if (map.isEmpty()) return;
    appendHelp(sb, name, "counter", help);
    map.forEach(
        (k, v) -> {
          String lbl =
              String.format(
                  "case_id=\"%s\",task_type=\"%s\",stage=\"%d\"",
                  escape(k.caseId()), escape(k.taskType()), k.stage());
          appendSample(sb, name, lbl, v.sum());
        });
  }

  private static void appendHelp(StringBuilder sb, String name, String type, String help) {
    sb.append("# HELP ").append(name).append(' ').append(help).append('\n');
    sb.append("# TYPE ").append(name).append(' ').append(type).append('\n');
  }

  private static void appendSample(StringBuilder sb, String name, String labels, long value) {
    sb.append(name).append('{').append(labels).append("} ").append(value).append('\n');
  }

  private static String agentLabels(AgentRegistration a) {
    return String.format(
        "agent_id=\"%s\",task_type=\"%s\",stage=\"%d\",hostname=\"%s\"",
        escape(a.getAgentId()),
        escape(a.getTaskType()),
        a.getStageNumber(),
        escape(a.getHostname()));
  }

  /**
   * Escapes a Prometheus label value per the text-format spec: backslash → {@code \\}, double-quote
   * → {@code \"}, newline → {@code \n}.
   */
  public static String escape(String s) {
    if (s == null) return "";
    return s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n");
  }

  private static String safe(String s) {
    return s != null ? s : "";
  }
}
