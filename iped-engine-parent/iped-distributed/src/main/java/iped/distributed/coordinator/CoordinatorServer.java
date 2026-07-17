package iped.distributed.coordinator;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import iped.distributed.audit.ProcessingAuditLog;
import iped.distributed.config.DistributedConfig;
import iped.distributed.dualrun.DualRunManager;
import iped.distributed.dualrun.DualRunReport;
import iped.distributed.dualrun.ItemSummary;
import iped.distributed.kafka.DlqAutoRetrier;
import iped.distributed.kafka.DlqEntry;
import iped.distributed.kafka.DlqManager;
import iped.distributed.kafka.DlqPosition;
import iped.distributed.kafka.DlqTopicStats;
import iped.distributed.kafka.TopicProvisioner;
import iped.distributed.metrics.ConsumerLagProvider;
import iped.distributed.metrics.DistributedMetrics;
import iped.distributed.status.ItemStatusConsumer;
import iped.distributed.status.ItemStatusProducer;
import iped.distributed.status.StatusTopicReplayer;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.jetty.ee10.servlet.ServletContextHandler;
import org.eclipse.jetty.ee10.servlet.ServletHolder;
import org.eclipse.jetty.server.Server;

/**
 * Lightweight Jetty-based REST server for the IPED distributed processing coordinator.
 *
 * <h3>Endpoints</h3>
 *
 * <pre>
 *   POST   /api/v1/agents/register           Register a new Task Agent
 *   POST   /api/v1/agents/{id}/heartbeat     Update agent heartbeat and load
 *   DELETE /api/v1/agents/{id}               Unregister agent
 *   GET    /api/v1/agents                    List all live agents
 *   GET    /api/v1/agents/availability       Aggregated availability per task type
 *
 *   POST   /api/v1/cases/start               Start a new case (provisions topics; optional "priority")
 *   GET    /api/v1/cases                     List all cases
 *   GET    /api/v1/cases/{caseId}            Case status
 *   GET    /api/v1/cases/{caseId}/progress   Case progress: discovered/completedFinal/inFlight/failed/pct
 *   POST   /api/v1/cases/{caseId}/priority   Re-prioritise a case — body: {"priority":"URGENT|HIGH|NORMAL|LOW"}
 *   DELETE /api/v1/cases/{caseId}            Delete a case and deprovision its Kafka topics
 *   POST   /api/v1/cases/{caseId}/pause      Pause a running case (agents stop receiving topics)
 *   POST   /api/v1/cases/{caseId}/resume     Resume a paused case
 *   POST   /api/v1/cases/{caseId}/tags       Merge key-value tags into case metadata
 *   POST   /api/v1/cases/batch-status        Batch progress for a list of case IDs
 *   GET    /api/v1/pipeline/{caseId}         Task → stage number map
 *
 *   GET    /api/v1/dlq/{caseId}             List DLQ entries (query param: limit, default 100)
 *   GET    /api/v1/dlq/{caseId}/count       Lightweight DLQ entry count for monitoring
 *   POST   /api/v1/dlq/{caseId}/requeue     Requeue items — body: {"positions":[…], "delayMs":0}
 *   POST   /api/v1/dlq/{caseId}/discard     Discard items — body: {"positions":[{dlqTopic,partition,offset},...]}
 *
 *   GET    /api/v1/health                   Coordinator readiness: status/activeCases/liveAgents/uptimeMs/
 *                                            brokerReachable. Verifies real Kafka broker connectivity
 *                                            (3s-timeout AdminClient.describeCluster probe); returns
 *                                            HTTP 200 {"status":"ok",...,"brokerReachable":true} when the
 *                                            broker answers, HTTP 503 {"status":"degraded",...,
 *                                            "brokerReachable":false} when it is unreachable/times out/
 *                                            errors (fail-open — never a raw 500 from this check).
 *
 *   GET    /api/v1/agents/{agentId}         Full registration details for a single agent
 *   GET    /api/v1/agents/summary           Pool summary grouped by taskType (includes pressure)
 *
 *   GET    /api/v1/dlq/{caseId}/stats       Per-topic DLQ statistics (count, oldest/newest offset)
 *
 *   GET    /api/v1/events                   Recent coordinator lifecycle events (ring buffer)
 *
 *   GET    /metrics                         Prometheus text-format metrics scrape (all cases)
 *
 *   POST   /api/v1/dualrun/{caseId}/start       Start a dual-run session for a case (auto if dualRunEnabled)
 *   POST   /api/v1/dualrun/{caseId}/reference   Submit the reference (monolithic) item list
 *   GET    /api/v1/dualrun/{caseId}             Get the current comparison report
 *
 *   GET    /api/v1/audit/{caseId}               Chain-of-custody audit log for a case (JSON)
 *   GET    /api/v1/audit/{caseId}/csv           Chain-of-custody audit log for a case (CSV export)
 *   GET    /api/v1/audit/{caseId}/{itemUuid}    All processing records for one item
 * </pre>
 */
@Slf4j
public class CoordinatorServer {

  private final DistributedConfig cfg;
  private final int port;
  private final AgentRegistry registry;
  private final CaseLifecycleManager lifecycle;
  private final ObjectMapper mapper;
  private final ScheduledExecutorService eviction;
  private final long startedAtMs = System.currentTimeMillis();
  private Server server;

  private ItemStatusProducer statusProducer;
  private ItemStatusConsumer statusConsumer;
  private CaseCompletionMonitor completionMonitor;
  private DlqManager dlqManager;
  private DlqAutoRetrier dlqAutoRetrier;
  private DistributedMetrics metrics;
  private ConsumerLagProvider lagProvider;
  private BrokerHealthProbe brokerHealthProbe;
  private DualRunManager dualRunManager;
  private ProcessingAuditLog auditLog;
  private AgentTopicAssigner topicAssigner;
  private CoordinatorEventLog eventLog;

  public CoordinatorServer(DistributedConfig cfg) {
    this.cfg = cfg;
    this.port = cfg.getCoordinatorPort();
    this.registry = new AgentRegistry(cfg.getAgentExpirySeconds());
    String stateDir = cfg.getCoordinatorStateDir();
    this.lifecycle =
        new CaseLifecycleManager(
            new TopicProvisioner(cfg.getKafkaBootstrapServers()),
            (stateDir != null && !stateDir.isBlank()) ? java.nio.file.Paths.get(stateDir) : null);
    this.mapper = new ObjectMapper().registerModule(new JavaTimeModule());
    this.eviction =
        Executors.newSingleThreadScheduledExecutor(r -> new Thread(r, "coordinator-eviction"));
  }

  // -----------------------------------------------------------------------

  public void start() throws Exception {
    server = new Server(port);

    ServletContextHandler ctx = new ServletContextHandler();
    ctx.setContextPath("/");
    server.setHandler(ctx);

    ctx.addServlet(new ServletHolder(new ApiServlet()), "/api/v1/*");
    ctx.addServlet(new ServletHolder(new MetricsServlet()), "/metrics");

    server.start();
    log.info("CoordinatorServer started on port {}", port);

    // Periodically log registry health
    eviction.scheduleAtFixedRate(
        () -> {
          Map<String, AgentAvailability> avail = registry.availability();
          if (!avail.isEmpty()) {
            avail.forEach(
                (type, a) ->
                    log.info(
                        "  [{}] agents={} free={}/{}",
                        type,
                        a.getTotalAgents(),
                        a.getFreeSlots(),
                        a.getTotalSlots()));
          }
        },
        30,
        30,
        TimeUnit.SECONDS);

    // Event log
    eventLog = new CoordinatorEventLog();

    // Topic steering: computes which topics to return in each heartbeat response
    topicAssigner = new AgentTopicAssigner(registry, lifecycle, cfg.getMaxSubscribedCases());

    // DLQ operator tooling
    dlqManager = new DlqManager(cfg.getKafkaBootstrapServers(), cfg.getDeadLetterTopicSuffix());

    // Metrics
    metrics = new DistributedMetrics();
    lagProvider = new ConsumerLagProvider(cfg.getKafkaBootstrapServers());

    // /health readiness probe: one long-lived AdminClient (same discipline as
    // ConsumerLagProvider) reused across requests instead of opening a new one per call.
    brokerHealthProbe = new BrokerHealthProbe(cfg.getKafkaBootstrapServers());

    // Timeout + automatic case-completion detection driven by iped.status
    statusProducer = new ItemStatusProducer(cfg.getKafkaBootstrapServers());
    completionMonitor =
        new CaseCompletionMonitor(lifecycle, statusProducer::publish, cfg.getItemTimeoutSeconds());

    // Crash recovery: when case definitions were reloaded from durable state, replay the
    // iped.status history to rebuild completion progress before going live. Best-effort —
    // a missing topic / broker just leaves progress empty (rebuilt from live events).
    if (!lifecycle.allCases().isEmpty()) {
      log.info(
          "Recovered {} case definition(s) from durable state — replaying iped.status "
              + "to rebuild completion progress",
          lifecycle.allCases().size());
      new StatusTopicReplayer(cfg.getKafkaBootstrapServers())
          .replayInto(completionMonitor::recoverSingle);
    }

    // Audit log (chain-of-custody)
    String auditPath = cfg.getAuditLogPath();
    auditLog =
        (auditPath != null && !auditPath.isBlank())
            ? new ProcessingAuditLog(java.nio.file.Path.of(auditPath))
            : new ProcessingAuditLog();

    // Dual-run manager: always created; sessions only auto-opened when dualRunEnabled
    dualRunManager = new DualRunManager();

    // Both the completion monitor, the metrics registry, and the dual-run manager
    // receive every status event
    statusConsumer =
        new ItemStatusConsumer(
            cfg.getKafkaBootstrapServers(),
            "iped-coordinator",
            false,
            event -> {
              completionMonitor.onEvent(event);
              metrics.recordEvent(event);
              dualRunManager.recordEvent(event);
              auditLog.record(event);
            });
    statusConsumer.start();
    eviction.scheduleAtFixedRate(
        () -> completionMonitor.sweepTimeouts(java.time.Instant.now()), 30, 30, TimeUnit.SECONDS);

    // DLQ auto-retry (disabled by default — dlqAutoRetryMaxAttempts = 0)
    dlqAutoRetrier = new DlqAutoRetrier(dlqManager, lifecycle, cfg);
    if (dlqAutoRetrier.isEnabled()) {
      int interval = cfg.getDlqAutoRetryIntervalSeconds();
      eviction.scheduleAtFixedRate(
          () -> {
            dlqAutoRetrier.sweep();
            eventLog.append(
                CoordinatorEventLog.CoordinatorEvent.generic(
                    CoordinatorEventLog.EventType.DLQ_AUTO_RETRY, "DLQ auto-retry sweep"));
          },
          interval,
          interval,
          TimeUnit.SECONDS);
      log.info(
          "DLQ auto-retry enabled (maxAttempts={}, delayMs={}, intervalSeconds={})",
          cfg.getDlqAutoRetryMaxAttempts(),
          cfg.getDlqAutoRetryDelayMs(),
          interval);
    }
  }

  public void stop() throws Exception {
    eviction.shutdown();
    if (statusConsumer != null) statusConsumer.close();
    if (statusProducer != null) statusProducer.close();
    if (dlqManager != null) dlqManager.close();
    if (lagProvider != null) lagProvider.close();
    if (brokerHealthProbe != null) brokerHealthProbe.close();
    if (server != null) server.stop();
  }

  public void join() throws InterruptedException {
    if (server != null) server.join();
  }

  // -----------------------------------------------------------------------
  // Servlet — routes all /api/v1/* requests
  // -----------------------------------------------------------------------

  private class ApiServlet extends HttpServlet {

    @Override
    protected void service(HttpServletRequest req, HttpServletResponse resp) throws IOException {
      resp.setContentType("application/json;charset=UTF-8");
      String method = req.getMethod();
      String path = req.getPathInfo(); // e.g. /agents/register
      if (path == null) path = "/";

      try {
        if ("POST".equals(method) && path.equals("/agents/register")) {
          handleRegister(req, resp);
        } else if ("POST".equals(method) && path.matches("/agents/[^/]+/heartbeat")) {
          handleHeartbeat(req, resp, extractSegment(path, 2));
        } else if ("DELETE".equals(method) && path.matches("/agents/[^/]+")) {
          handleUnregister(resp, extractSegment(path, 2));
        } else if ("GET".equals(method) && path.equals("/agents")) {
          handleListAgents(resp);
        } else if ("GET".equals(method) && path.equals("/agents/availability")) {
          handleAvailability(resp);
        } else if ("GET".equals(method) && path.equals("/agents/summary")) {
          handleAgentSummary(resp);
        } else if ("GET".equals(method) && path.matches("/agents/[^/]+")) {
          handleAgentDetail(resp, extractSegment(path, 2));
        } else if ("POST".equals(method) && path.equals("/cases/start")) {
          handleStartCase(req, resp);
        } else if ("GET".equals(method) && path.equals("/cases")) {
          handleListCases(resp);
        } else if ("POST".equals(method) && path.equals("/cases/batch-status")) {
          handleBatchStatus(req, resp);
        } else if ("GET".equals(method) && path.matches("/cases/[^/]+/progress")) {
          handleCaseProgress(resp, extractSegment(path, 2));
        } else if ("POST".equals(method) && path.matches("/cases/[^/]+/pause")) {
          handleCasePause(resp, extractSegment(path, 2));
        } else if ("POST".equals(method) && path.matches("/cases/[^/]+/resume")) {
          handleCaseResume(resp, extractSegment(path, 2));
        } else if ("POST".equals(method) && path.matches("/cases/[^/]+/tags")) {
          handleCaseTags(req, resp, extractSegment(path, 2));
        } else if ("POST".equals(method) && path.matches("/cases/[^/]+/priority")) {
          handleSetPriority(req, resp, extractSegment(path, 2));
        } else if ("DELETE".equals(method) && path.matches("/cases/[^/]+")) {
          handleDeleteCase(resp, extractSegment(path, 2));
        } else if ("GET".equals(method) && path.matches("/cases/[^/]+")) {
          handleCaseStatus(resp, extractSegment(path, 2));
        } else if ("GET".equals(method) && path.equals("/health")) {
          handleHealth(resp);
        } else if ("GET".equals(method) && path.matches("/pipeline/[^/]+")) {
          handlePipeline(resp, extractSegment(path, 2));
        } else if ("GET".equals(method) && path.matches("/dlq/[^/]+/stats")) {
          handleDlqStats(resp, extractSegment(path, 2));
        } else if ("GET".equals(method) && path.matches("/dlq/[^/]+/count")) {
          handleDlqCount(resp, extractSegment(path, 2));
        } else if ("GET".equals(method) && path.matches("/dlq/[^/]+")) {
          handleDlqList(req, resp, extractSegment(path, 2));
        } else if ("POST".equals(method) && path.matches("/dlq/[^/]+/requeue")) {
          handleDlqRequeue(req, resp, extractSegment(path, 2));
        } else if ("POST".equals(method) && path.matches("/dlq/[^/]+/discard")) {
          handleDlqDiscard(req, resp, extractSegment(path, 2));
        } else if ("POST".equals(method) && path.matches("/dualrun/[^/]+/start")) {
          handleDualRunStart(resp, extractSegment(path, 2));
        } else if ("POST".equals(method) && path.matches("/dualrun/[^/]+/reference")) {
          handleDualRunSubmitReference(req, resp, extractSegment(path, 2));
        } else if ("GET".equals(method) && path.matches("/dualrun/[^/]+")) {
          handleDualRunReport(resp, extractSegment(path, 2));
        } else if ("GET".equals(method) && path.matches("/audit/[^/]+/csv")) {
          handleAuditCsv(resp, extractSegment(path, 2));
        } else if ("GET".equals(method) && path.matches("/audit/[^/]+/[^/]+")) {
          handleAuditItem(resp, extractSegment(path, 2), extractSegment(path, 3));
        } else if ("GET".equals(method) && path.matches("/audit/[^/]+")) {
          handleAuditCase(resp, extractSegment(path, 2));
        } else if ("GET".equals(method) && path.equals("/events")) {
          handleEvents(req, resp);
        } else {
          resp.setStatus(404);
          resp.getWriter().write("{\"error\":\"Not found\"}");
        }
      } catch (Exception e) {
        log.error("Request error: {} {}", method, path, e);
        resp.setStatus(500);
        resp.getWriter().write("{\"error\":\"" + e.getMessage() + "\"}");
      }
    }

    private void handleRegister(HttpServletRequest req, HttpServletResponse resp)
        throws IOException {
      AgentRegistration reg = mapper.readValue(req.getInputStream(), AgentRegistration.class);
      registry.register(reg);
      resp.setStatus(200);
      resp.getWriter().write("{\"status\":\"registered\"}");
    }

    private void handleHeartbeat(HttpServletRequest req, HttpServletResponse resp, String agentId)
        throws IOException {
      @SuppressWarnings("unchecked")
      Map<String, Object> body = mapper.readValue(req.getInputStream(), Map.class);
      int freeSlots = body.containsKey("freeSlots") ? (int) body.get("freeSlots") : 0;
      int currentLoad = body.containsKey("currentLoad") ? (int) body.get("currentLoad") : 0;
      iped.distributed.resource.PressureLevel level = parsePressure(body.get("pressureLevel"));
      double ratio = body.get("pressureRatio") instanceof Number n ? n.doubleValue() : 0.0;
      registry.heartbeat(agentId, freeSlots, currentLoad, level, ratio);

      // Return the topic list the agent should subscribe to (multi-case steering).
      // topicAssigner is null until start() has been called (e.g. in unit tests that
      // construct CoordinatorServer but never call start()); guard defensively.
      List<String> topics =
          (topicAssigner != null) ? topicAssigner.topicsForAgent(agentId) : List.of();
      resp.setStatus(200);
      resp.getWriter()
          .write(
              "{\"status\":\"ok\",\"subscribedTopics\":" + mapper.writeValueAsString(topics) + "}");
    }

    private iped.distributed.resource.PressureLevel parsePressure(Object raw) {
      if (raw == null) return iped.distributed.resource.PressureLevel.NONE;
      try {
        return iped.distributed.resource.PressureLevel.valueOf(raw.toString().trim().toUpperCase());
      } catch (IllegalArgumentException e) {
        return iped.distributed.resource.PressureLevel.NONE;
      }
    }

    private void handleUnregister(HttpServletResponse resp, String agentId) throws IOException {
      registry.unregister(agentId);
      resp.setStatus(204);
    }

    private void handleListAgents(HttpServletResponse resp) throws IOException {
      resp.setStatus(200);
      mapper.writeValue(resp.getWriter(), registry.liveAgents());
    }

    private void handleAvailability(HttpServletResponse resp) throws IOException {
      resp.setStatus(200);
      mapper.writeValue(resp.getWriter(), registry.availability());
    }

    private void handleStartCase(HttpServletRequest req, HttpServletResponse resp)
        throws IOException {
      @SuppressWarnings("unchecked")
      Map<String, Object> body = mapper.readValue(req.getInputStream(), Map.class);
      String caseId = (String) body.get("caseId");
      @SuppressWarnings("unchecked")
      List<String> tasks = (List<String>) body.get("orderedTaskNames");
      int partitions = body.containsKey("topicPartitions") ? (int) body.get("topicPartitions") : 8;
      int replication =
          body.containsKey("replicationFactor") ? (int) body.get("replicationFactor") : 1;
      iped.distributed.scheduler.CasePriority priority =
          iped.distributed.scheduler.CasePriority.parseOrDefault((String) body.get("priority"));
      lifecycle.startCase(caseId, tasks, partitions, (short) replication, priority);
      if (cfg.isDualRunEnabled()) {
        dualRunManager.startSession(caseId);
      }
      eventLog.append(
          CoordinatorEventLog.CoordinatorEvent.caseEvent(
              CoordinatorEventLog.EventType.CASE_STARTED,
              caseId,
              "Started with " + tasks.size() + " tasks, priority=" + priority));
      resp.setStatus(201);
      resp.getWriter()
          .write(
              "{\"caseId\":\""
                  + caseId
                  + "\",\"status\":\"started\",\"priority\":\""
                  + priority
                  + "\",\"dualRun\":"
                  + cfg.isDualRunEnabled()
                  + "}");
    }

    private void handleSetPriority(HttpServletRequest req, HttpServletResponse resp, String caseId)
        throws IOException {
      @SuppressWarnings("unchecked")
      Map<String, Object> body = mapper.readValue(req.getInputStream(), Map.class);
      iped.distributed.scheduler.CasePriority priority =
          iped.distributed.scheduler.CasePriority.parseOrDefault((String) body.get("priority"));
      boolean updated = lifecycle.setCasePriority(caseId, priority);
      if (!updated) {
        resp.setStatus(404);
        resp.getWriter().write("{\"error\":\"Unknown case: " + caseId + "\"}");
        return;
      }
      resp.setStatus(200);
      resp.getWriter().write("{\"caseId\":\"" + caseId + "\",\"priority\":\"" + priority + "\"}");
    }

    private void handleListCases(HttpServletResponse resp) throws IOException {
      resp.setStatus(200);
      mapper.writeValue(resp.getWriter(), lifecycle.allCases());
    }

    private void handleCaseStatus(HttpServletResponse resp, String caseId) throws IOException {
      CaseLifecycleManager.CaseStatus status = lifecycle.getStatus(caseId);
      if (status == null) {
        resp.setStatus(404);
        resp.getWriter().write("{}");
        return;
      }
      resp.setStatus(200);
      mapper.writeValue(resp.getWriter(), status);
    }

    private void handleCaseProgress(HttpServletResponse resp, String caseId) throws IOException {
      if (lifecycle.getStatus(caseId) == null) {
        resp.setStatus(404);
        resp.getWriter().write("{\"error\":\"Unknown case: " + caseId + "\"}");
        return;
      }
      long discovered = completionMonitor.discoveredCount(caseId);
      long completed = completionMonitor.completedFinalCount(caseId);
      long inFlight = completionMonitor.inFlightCount(caseId);
      long failed = completionMonitor.failedCount(caseId);
      double pct = discovered > 0 ? (completed * 100.0 / discovered) : 0.0;
      resp.setStatus(200);
      resp.getWriter()
          .write(
              String.format(
                  java.util.Locale.ROOT,
                  "{\"caseId\":\"%s\",\"discovered\":%d,\"completedFinal\":%d,"
                      + "\"inFlight\":%d,\"failed\":%d,\"completionPct\":%.2f}",
                  caseId,
                  discovered,
                  completed,
                  inFlight,
                  failed,
                  pct));
    }

    private void handleDeleteCase(HttpServletResponse resp, String caseId) throws IOException {
      boolean deleted = lifecycle.deleteCase(caseId);
      if (!deleted) {
        resp.setStatus(404);
        resp.getWriter().write("{\"error\":\"Unknown case: " + caseId + "\"}");
        return;
      }
      eventLog.append(
          CoordinatorEventLog.CoordinatorEvent.caseEvent(
              CoordinatorEventLog.EventType.CASE_DELETED, caseId, "Deleted by operator"));
      resp.setStatus(200);
      resp.getWriter().write("{\"caseId\":\"" + caseId + "\",\"status\":\"deleted\"}");
    }

    /**
     * Readiness check: liveness (process is up, serving requests) plus a real Kafka broker
     * connectivity probe (PI-1-F3-S1). Never throws — a probe failure (timeout, broker down,
     * unexpected error) is reported as {@code brokerReachable:false} / HTTP 503, not propagated as
     * a 500.
     */
    private void handleHealth(HttpServletResponse resp) throws IOException {
      long activeCases =
          lifecycle.allCases().stream()
              .filter(s -> s.state == CaseLifecycleManager.CaseStatus.State.RUNNING)
              .count();
      int liveAgents = registry.liveAgents().size();
      long uptimeMs = System.currentTimeMillis() - startedAtMs;
      // brokerHealthProbe is only null if /health is somehow reached before start()
      // finished initialising it (should not happen via the real Jetty servlet, but
      // fail closed rather than lying about readiness).
      BrokerHealthProbe.ProbeResult probe =
          (brokerHealthProbe != null)
              ? brokerHealthProbe.probe()
              : BrokerHealthProbe.ProbeResult.degraded("Health probe not yet initialised");
      boolean brokerReachable = probe.reachable();
      String status = brokerReachable ? "ok" : "degraded";
      resp.setStatus(brokerReachable ? 200 : 503);
      StringBuilder json =
          new StringBuilder(
              String.format(
                  java.util.Locale.ROOT,
                  "{\"status\":\"%s\",\"activeCases\":%d,\"liveAgents\":%d,\"uptimeMs\":%d,"
                      + "\"brokerReachable\":%b",
                  status,
                  activeCases,
                  liveAgents,
                  uptimeMs,
                  brokerReachable));
      if (!brokerReachable && probe.reason() != null) {
        json.append(",\"reason\":").append(mapper.writeValueAsString(probe.reason()));
      }
      json.append('}');
      resp.getWriter().write(json.toString());
    }

    private void handleAgentDetail(HttpServletResponse resp, String agentId) throws IOException {
      AgentRegistration reg = registry.getAgent(agentId);
      if (reg == null) {
        resp.setStatus(404);
        resp.getWriter().write("{\"error\":\"Unknown agent: " + agentId + "\"}");
        return;
      }
      resp.setStatus(200);
      mapper.writeValue(resp.getWriter(), reg);
    }

    private void handleAgentSummary(HttpServletResponse resp) throws IOException {
      List<AgentRegistration> agents = registry.liveAgents();
      java.util.Map<String, java.util.Map<String, Object>> byType = new java.util.LinkedHashMap<>();
      for (AgentRegistration r : agents) {
        String type = r.getTaskType();
        java.util.Map<String, Object> entry =
            byType.computeIfAbsent(
                type,
                k -> {
                  java.util.Map<String, Object> m = new java.util.LinkedHashMap<>();
                  m.put("taskType", k);
                  m.put("agentCount", 0);
                  m.put("totalSlots", 0);
                  m.put("freeSlots", 0);
                  m.put("pressuredAgents", 0);
                  return m;
                });
        entry.put("agentCount", (int) entry.get("agentCount") + 1);
        entry.put("totalSlots", (int) entry.get("totalSlots") + r.getMaxParallelItems());
        entry.put("freeSlots", (int) entry.get("freeSlots") + r.getFreeSlots());
        if (!r.isSchedulable())
          entry.put("pressuredAgents", (int) entry.get("pressuredAgents") + 1);
      }
      resp.setStatus(200);
      mapper.writeValue(resp.getWriter(), byType.values());
    }

    private void handleCasePause(HttpServletResponse resp, String caseId) throws IOException {
      boolean ok = lifecycle.pauseCase(caseId);
      if (!ok) {
        resp.setStatus(404);
        resp.getWriter().write("{\"error\":\"Cannot pause: " + caseId + "\"}");
        return;
      }
      eventLog.append(
          CoordinatorEventLog.CoordinatorEvent.caseEvent(
              CoordinatorEventLog.EventType.CASE_PAUSED, caseId, "Case paused by operator"));
      resp.setStatus(200);
      resp.getWriter().write("{\"caseId\":\"" + caseId + "\",\"status\":\"paused\"}");
    }

    private void handleCaseResume(HttpServletResponse resp, String caseId) throws IOException {
      boolean ok = lifecycle.resumeCase(caseId);
      if (!ok) {
        resp.setStatus(404);
        resp.getWriter().write("{\"error\":\"Cannot resume: " + caseId + "\"}");
        return;
      }
      eventLog.append(
          CoordinatorEventLog.CoordinatorEvent.caseEvent(
              CoordinatorEventLog.EventType.CASE_RESUMED, caseId, "Case resumed by operator"));
      resp.setStatus(200);
      resp.getWriter().write("{\"caseId\":\"" + caseId + "\",\"status\":\"running\"}");
    }

    @SuppressWarnings("unchecked")
    private void handleCaseTags(HttpServletRequest req, HttpServletResponse resp, String caseId)
        throws IOException {
      java.util.Map<String, Object> body =
          mapper.readValue(req.getInputStream(), java.util.Map.class);
      java.util.Map<String, String> tags = new java.util.LinkedHashMap<>();
      Object raw = body.get("tags");
      if (raw instanceof java.util.Map<?, ?> m) {
        m.forEach((k, v) -> tags.put(String.valueOf(k), String.valueOf(v)));
      }
      boolean ok = lifecycle.updateTags(caseId, tags);
      if (!ok) {
        resp.setStatus(404);
        resp.getWriter().write("{\"error\":\"Unknown case: " + caseId + "\"}");
        return;
      }
      resp.setStatus(200);
      resp.getWriter().write("{\"caseId\":\"" + caseId + "\",\"tagCount\":" + tags.size() + "}");
    }

    @SuppressWarnings("unchecked")
    private void handleBatchStatus(HttpServletRequest req, HttpServletResponse resp)
        throws IOException {
      java.util.Map<String, Object> body =
          mapper.readValue(req.getInputStream(), java.util.Map.class);
      java.util.List<String> ids =
          (java.util.List<String>) body.getOrDefault("caseIds", java.util.List.of());
      java.util.List<java.util.Map<String, Object>> results = new java.util.ArrayList<>();
      for (String id : ids) {
        CaseLifecycleManager.CaseStatus st = lifecycle.getStatus(id);
        java.util.Map<String, Object> entry = new java.util.LinkedHashMap<>();
        entry.put("caseId", id);
        if (st == null) {
          entry.put("error", "not found");
          results.add(entry);
          continue;
        }
        entry.put("state", st.state);
        long disc = completionMonitor.discoveredCount(id);
        long comp = completionMonitor.completedFinalCount(id);
        entry.put("discovered", disc);
        entry.put("completedFinal", comp);
        entry.put("inFlight", completionMonitor.inFlightCount(id));
        entry.put("failed", completionMonitor.failedCount(id));
        entry.put(
            "completionPct",
            disc > 0 ? String.format(java.util.Locale.ROOT, "%.2f", comp * 100.0 / disc) : "0.00");
        results.add(entry);
      }
      resp.setStatus(200);
      mapper.writeValue(resp.getWriter(), results);
    }

    private void handleDlqStats(HttpServletResponse resp, String caseId) throws Exception {
      java.util.List<DlqTopicStats> stats = dlqManager.topicStats(caseId);
      resp.setStatus(200);
      mapper.writeValue(resp.getWriter(), stats);
    }

    private void handleEvents(HttpServletRequest req, HttpServletResponse resp) throws IOException {
      String limitParam = req.getParameter("limit");
      int limit = limitParam != null ? Integer.parseInt(limitParam) : 50;
      resp.setStatus(200);
      mapper.writeValue(resp.getWriter(), eventLog.latest(limit));
    }

    private void handleDlqCount(HttpServletResponse resp, String caseId) throws Exception {
      long count = dlqManager.count(caseId);
      resp.setStatus(200);
      resp.getWriter().write("{\"caseId\":\"" + caseId + "\",\"count\":" + count + "}");
    }

    private void handlePipeline(HttpServletResponse resp, String caseId) throws IOException {
      try {
        List<String> tasks = lifecycle.getTaskOrder(caseId);
        Map<String, Integer> stageMap = new LinkedHashMap<>();
        for (int i = 0; i < tasks.size(); i++) stageMap.put(tasks.get(i), i);
        resp.setStatus(200);
        mapper.writeValue(resp.getWriter(), stageMap);
      } catch (IllegalArgumentException e) {
        resp.setStatus(404);
        resp.getWriter().write("{\"error\":\"" + e.getMessage() + "\"}");
      }
    }

    private void handleDlqList(HttpServletRequest req, HttpServletResponse resp, String caseId)
        throws Exception {
      String limitParam = req.getParameter("limit");
      int limit =
          (limitParam != null) ? Integer.parseInt(limitParam) : DlqManager.DEFAULT_LIST_LIMIT;
      List<DlqEntry> entries = dlqManager.list(caseId, limit);
      resp.setStatus(200);
      mapper.writeValue(resp.getWriter(), entries);
    }

    @SuppressWarnings("unchecked")
    private void handleDlqRequeue(HttpServletRequest req, HttpServletResponse resp, String caseId)
        throws IOException {
      Map<String, Object> body = mapper.readValue(req.getInputStream(), Map.class);
      List<Map<String, Object>> rawPos = (List<Map<String, Object>>) body.get("positions");
      List<DlqPosition> positions = new ArrayList<>();
      if (rawPos != null) {
        for (Map<String, Object> entry : rawPos) {
          String topic = (String) entry.get("dlqTopic");
          int partition = (int) entry.get("partition");
          long offset = ((Number) entry.get("offset")).longValue();
          positions.add(new DlqPosition(topic, partition, offset));
        }
      }
      long delayMs = body.get("delayMs") instanceof Number n ? n.longValue() : 0L;
      int count = dlqManager.requeue(caseId, positions, delayMs);
      resp.setStatus(200);
      resp.getWriter().write("{\"requeued\":" + count + ",\"delayMs\":" + delayMs + "}");
    }

    private void handleDlqDiscard(HttpServletRequest req, HttpServletResponse resp, String caseId)
        throws IOException {
      List<DlqPosition> positions = parsePositions(req);
      int count = dlqManager.discard(caseId, positions);
      resp.setStatus(200);
      resp.getWriter().write("{\"discarded\":" + count + "}");
    }

    private void handleAuditCase(HttpServletResponse resp, String caseId) throws IOException {
      resp.setStatus(200);
      mapper.writeValue(resp.getWriter(), auditLog.forCase(caseId));
    }

    private void handleAuditCsv(HttpServletResponse resp, String caseId) throws IOException {
      resp.setContentType("text/csv;charset=UTF-8");
      resp.setHeader("Content-Disposition", "attachment; filename=\"audit-" + caseId + ".csv\"");
      resp.setStatus(200);
      resp.getWriter().write(auditLog.exportCsv(caseId));
    }

    private void handleAuditItem(HttpServletResponse resp, String caseId, String itemUuid)
        throws IOException {
      resp.setStatus(200);
      mapper.writeValue(resp.getWriter(), auditLog.forItem(caseId, itemUuid));
    }

    private void handleDualRunStart(HttpServletResponse resp, String caseId) throws IOException {
      dualRunManager.startSession(caseId);
      resp.setStatus(200);
      resp.getWriter().write("{\"caseId\":\"" + caseId + "\",\"status\":\"session-open\"}");
    }

    private void handleDualRunSubmitReference(
        HttpServletRequest req, HttpServletResponse resp, String caseId) throws IOException {
      @SuppressWarnings("unchecked")
      Map<String, Object> body = mapper.readValue(req.getInputStream(), Map.class);
      @SuppressWarnings("unchecked")
      List<Map<String, Object>> rawItems = (List<Map<String, Object>>) body.get("items");
      if (rawItems == null) {
        resp.setStatus(400);
        resp.getWriter().write("{\"error\":\"missing 'items' array\"}");
        return;
      }
      List<ItemSummary> items = new ArrayList<>();
      for (Map<String, Object> raw : rawItems) {
        String path = (String) raw.get("path");
        String mediaType = (String) raw.get("mediaType");
        Long length = raw.get("lengthBytes") instanceof Number n ? n.longValue() : null;
        items.add(ItemSummary.reference(path, mediaType, length));
      }
      boolean ok = dualRunManager.submitReference(caseId, items);
      if (!ok) {
        dualRunManager.startSession(caseId);
        dualRunManager.submitReference(caseId, items);
      }
      resp.setStatus(200);
      resp.getWriter()
          .write("{\"caseId\":\"" + caseId + "\",\"referenceItemCount\":" + items.size() + "}");
    }

    private void handleDualRunReport(HttpServletResponse resp, String caseId) throws IOException {
      DualRunReport report = dualRunManager.getReport(caseId);
      if (report == null) {
        resp.setStatus(404);
        resp.getWriter().write("{\"error\":\"No dual-run session for case: " + caseId + "\"}");
        return;
      }
      resp.setStatus(200);
      mapper.writeValue(resp.getWriter(), report);
    }

    @SuppressWarnings("unchecked")
    private List<DlqPosition> parsePositions(HttpServletRequest req) throws IOException {
      Map<String, Object> body = mapper.readValue(req.getInputStream(), Map.class);
      List<Map<String, Object>> raw = (List<Map<String, Object>>) body.get("positions");
      if (raw == null) return List.of();
      List<DlqPosition> positions = new ArrayList<>();
      for (Map<String, Object> entry : raw) {
        String topic = (String) entry.get("dlqTopic");
        int partition = (int) entry.get("partition");
        long offset = ((Number) entry.get("offset")).longValue();
        positions.add(new DlqPosition(topic, partition, offset));
      }
      return positions;
    }

    private String extractSegment(String path, int index) {
      String[] parts = path.split("/");
      return parts.length > index ? parts[index] : "";
    }
  }

  // -----------------------------------------------------------------------
  // Metrics servlet — GET /metrics
  // -----------------------------------------------------------------------

  private class MetricsServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
      // Collect consumer lag for all active cases (best-effort — empty map on failure)
      List<String> caseIds =
          lifecycle.allCases().stream().map(s -> s.caseId).collect(Collectors.toList());
      Map<DistributedMetrics.LagKey, Long> lag = lagProvider.getLagForCases(caseIds);

      // DLQ depth and stall status per active (RUNNING) case only — same scoping as
      // /health (see handleHealth above): a COMPLETED/FAILED/PAUSED case has no active
      // work, so computing/publishing its DLQ-count and stall gauges is both misleading
      // (a completed case would show case_stalled=1 forever) and needless per-scrape
      // cost across the coordinator's entire case history.
      // (best-effort; both providers never throw — DlqManager#count and
      // CaseCompletionMonitor#isStalled degrade to 0/false on error rather than failing
      // the whole scrape).
      List<String> activeCaseIds =
          lifecycle.allCases().stream()
              .filter(s -> s.state == CaseLifecycleManager.CaseStatus.State.RUNNING)
              .map(s -> s.caseId)
              .collect(Collectors.toList());
      long stallWindowMs = cfg.getItemTimeoutSeconds() * 1000L;
      // Batched across all active cases: a handful of AdminClient round-trips total
      // instead of a describeTopics+listOffsets sequence per case (see DlqManager#countForCases).
      Map<String, Long> dlqCountByCase = dlqManager.countForCases(activeCaseIds);
      Map<String, Boolean> stalledByCase = new java.util.LinkedHashMap<>();
      for (String caseId : activeCaseIds) {
        stalledByCase.put(caseId, completionMonitor.isStalled(caseId, stallWindowMs));
      }

      String body = metrics.scrape(registry, lag, dlqCountByCase, stalledByCase);
      resp.setContentType("text/plain; version=0.0.4; charset=utf-8");
      resp.setStatus(200);
      resp.getWriter().write(body);
    }
  }

  // -----------------------------------------------------------------------
  // Standalone entry point (run coordinator independently)
  // -----------------------------------------------------------------------

  /**
   * Standalone coordinator startup.
   *
   * <p>Configuration priority (highest → lowest):
   *
   * <ol>
   *   <li>Environment variables ({@code KAFKA_BOOTSTRAP_SERVERS}, {@code COORDINATOR_PORT}, {@code
   *       AGENT_EXPIRY_SECONDS}, {@code TOPIC_PARTITIONS}, {@code TOPIC_REPLICATION_FACTOR}, {@code
   *       HEARTBEAT_INTERVAL_SECONDS}, {@code ITEM_TIMEOUT_SECONDS})
   *   <li>Built-in defaults ({@link DistributedConfig} field initialisers)
   * </ol>
   *
   * <p>No argument is required. Useful for Docker / k8s deployments where all settings come from
   * env vars or a mounted {@code DistributedConfig.toml} (pass the config directory as the first
   * argument to load the file).
   */
  public static void main(String[] args) throws Exception {
    DistributedConfig cfg = new DistributedConfig();

    // 1. Apply env-var overrides (Docker / k8s friendly)
    iped.utils.UTF8Properties envProps = new iped.utils.UTF8Properties();
    applyEnv(envProps, "kafkaBootstrapServers", "KAFKA_BOOTSTRAP_SERVERS");
    applyEnv(envProps, "coordinatorPort", "COORDINATOR_PORT");
    applyEnv(envProps, "agentExpirySeconds", "AGENT_EXPIRY_SECONDS");
    applyEnv(envProps, "topicPartitions", "TOPIC_PARTITIONS");
    applyEnv(envProps, "topicReplicationFactor", "TOPIC_REPLICATION_FACTOR");
    applyEnv(envProps, "heartbeatIntervalSeconds", "HEARTBEAT_INTERVAL_SECONDS");
    applyEnv(envProps, "itemTimeoutSeconds", "ITEM_TIMEOUT_SECONDS");
    cfg.processProperties(envProps);

    // 2. Optionally load DistributedConfig.toml from a config directory (first arg)
    if (args.length > 0) {
      java.nio.file.Path configDir = java.nio.file.Paths.get(args[0]);
      java.io.File configFile = configDir.resolve(DistributedConfig.CONFIG_FILE).toFile();
      if (configFile.isFile()) {
        iped.utils.TomlProperties fileProps = new iped.utils.TomlProperties();
        fileProps.load(configFile.toPath());
        cfg.processProperties(fileProps);
        log.info("Loaded configuration from {}", configFile);
      } else {
        log.warn("Config file not found at {} — using defaults/env vars", configFile);
      }
    }

    java.util.List<String> configErrors = iped.distributed.config.ConfigValidator.validate(cfg);
    if (!configErrors.isEmpty()) {
      configErrors.forEach(e -> log.error("Config error: {}", e));
      throw new IllegalStateException(
          "Invalid DistributedConfig — " + configErrors.size() + " error(s), refusing to start");
    }

    log.info(
        "Starting IPED Coordinator — kafka={}, port={}",
        cfg.getKafkaBootstrapServers(),
        cfg.getCoordinatorPort());

    CoordinatorServer srv = new CoordinatorServer(cfg);
    Runtime.getRuntime()
        .addShutdownHook(
            new Thread(
                () -> {
                  try {
                    srv.stop();
                  } catch (Exception ignored) {
                  }
                }));
    srv.start();
    srv.join();
  }

  /** Copies a single env var into a {@link iped.utils.UTF8Properties} map if it is set. */
  private static void applyEnv(iped.utils.UTF8Properties props, String key, String envVar) {
    String val = System.getenv(envVar);
    if (val != null && !val.isBlank()) props.setProperty(key, val.trim());
  }
}
