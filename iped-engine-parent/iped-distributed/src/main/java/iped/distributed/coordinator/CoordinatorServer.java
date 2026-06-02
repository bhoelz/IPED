package iped.distributed.coordinator;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import iped.distributed.config.DistributedConfig;
import iped.distributed.kafka.TopicProvisioner;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.eclipse.jetty.ee10.servlet.ServletContextHandler;
import org.eclipse.jetty.ee10.servlet.ServletHolder;
import org.eclipse.jetty.server.Server;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Lightweight Jetty-based REST server for the IPED distributed processing coordinator.
 *
 * <h3>Endpoints</h3>
 * <pre>
 *   POST   /api/v1/agents/register           Register a new Task Agent
 *   POST   /api/v1/agents/{id}/heartbeat     Update agent heartbeat and load
 *   DELETE /api/v1/agents/{id}               Unregister agent
 *   GET    /api/v1/agents                    List all live agents
 *   GET    /api/v1/agents/availability       Aggregated availability per task type
 *
 *   POST   /api/v1/cases/start               Start a new case (provisions topics)
 *   GET    /api/v1/cases                     List all cases
 *   GET    /api/v1/cases/{caseId}            Case status
 *   GET    /api/v1/pipeline/{caseId}         Task → stage number map
 * </pre>
 */
public class CoordinatorServer {

    private static final Logger LOGGER = LoggerFactory.getLogger(CoordinatorServer.class);

    private final int                    port;
    private final AgentRegistry          registry;
    private final CaseLifecycleManager   lifecycle;
    private final ObjectMapper           mapper;
    private final ScheduledExecutorService eviction;
    private Server                       server;

    public CoordinatorServer(DistributedConfig cfg) {
        this.port      = cfg.getCoordinatorPort();
        this.registry  = new AgentRegistry(cfg.getAgentExpirySeconds());
        this.lifecycle = new CaseLifecycleManager(
                new TopicProvisioner(cfg.getKafkaBootstrapServers()));
        this.mapper    = new ObjectMapper().registerModule(new JavaTimeModule());
        this.eviction  = Executors.newSingleThreadScheduledExecutor(
                r -> new Thread(r, "coordinator-eviction"));
    }

    // -----------------------------------------------------------------------

    public void start() throws Exception {
        server = new Server(port);

        ServletContextHandler ctx = new ServletContextHandler();
        ctx.setContextPath("/");
        server.setHandler(ctx);

        ctx.addServlet(new ServletHolder(new ApiServlet()), "/api/v1/*");

        server.start();
        LOGGER.info("CoordinatorServer started on port {}", port);

        // Periodically log registry health
        eviction.scheduleAtFixedRate(() -> {
            Map<String, AgentAvailability> avail = registry.availability();
            if (!avail.isEmpty()) {
                avail.forEach((type, a) ->
                    LOGGER.info("  [{}] agents={} free={}/{}", type,
                            a.getTotalAgents(), a.getFreeSlots(), a.getTotalSlots()));
            }
        }, 30, 30, TimeUnit.SECONDS);
    }

    public void stop() throws Exception {
        eviction.shutdown();
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
            String path   = req.getPathInfo();          // e.g. /agents/register
            if (path == null) path = "/";

            try {
                if ("POST".equals(method)   && path.equals("/agents/register")) {
                    handleRegister(req, resp);
                } else if ("POST".equals(method)   && path.matches("/agents/[^/]+/heartbeat")) {
                    handleHeartbeat(req, resp, extractSegment(path, 2));
                } else if ("DELETE".equals(method) && path.matches("/agents/[^/]+")) {
                    handleUnregister(resp, extractSegment(path, 2));
                } else if ("GET".equals(method)    && path.equals("/agents")) {
                    handleListAgents(resp);
                } else if ("GET".equals(method)    && path.equals("/agents/availability")) {
                    handleAvailability(resp);
                } else if ("POST".equals(method)   && path.equals("/cases/start")) {
                    handleStartCase(req, resp);
                } else if ("GET".equals(method)    && path.equals("/cases")) {
                    handleListCases(resp);
                } else if ("GET".equals(method)    && path.matches("/cases/[^/]+")) {
                    handleCaseStatus(resp, extractSegment(path, 2));
                } else if ("GET".equals(method)    && path.matches("/pipeline/[^/]+")) {
                    handlePipeline(resp, extractSegment(path, 2));
                } else {
                    resp.setStatus(404);
                    resp.getWriter().write("{\"error\":\"Not found\"}");
                }
            } catch (Exception e) {
                LOGGER.error("Request error: {} {}", method, path, e);
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
            int freeSlots   = body.containsKey("freeSlots")   ? (int) body.get("freeSlots")   : 0;
            int currentLoad = body.containsKey("currentLoad") ? (int) body.get("currentLoad") : 0;
            registry.heartbeat(agentId, freeSlots, currentLoad);
            resp.setStatus(200);
            resp.getWriter().write("{\"status\":\"ok\"}");
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
            String caseId    = (String) body.get("caseId");
            @SuppressWarnings("unchecked")
            List<String> tasks = (List<String>) body.get("orderedTaskNames");
            int partitions    = body.containsKey("topicPartitions")   ? (int) body.get("topicPartitions")   : 8;
            int replication   = body.containsKey("replicationFactor") ? (int) body.get("replicationFactor") : 1;
            lifecycle.startCase(caseId, tasks, partitions, (short) replication);
            resp.setStatus(201);
            resp.getWriter().write("{\"caseId\":\"" + caseId + "\",\"status\":\"started\"}");
        }

        private void handleListCases(HttpServletResponse resp) throws IOException {
            resp.setStatus(200);
            mapper.writeValue(resp.getWriter(), lifecycle.allCases());
        }

        private void handleCaseStatus(HttpServletResponse resp, String caseId) throws IOException {
            CaseLifecycleManager.CaseStatus status = lifecycle.getStatus(caseId);
            if (status == null) { resp.setStatus(404); resp.getWriter().write("{}"); return; }
            resp.setStatus(200);
            mapper.writeValue(resp.getWriter(), status);
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

        private String extractSegment(String path, int index) {
            String[] parts = path.split("/");
            return parts.length > index ? parts[index] : "";
        }
    }

    // -----------------------------------------------------------------------
    // Standalone entry point (run coordinator independently)
    // -----------------------------------------------------------------------

    public static void main(String[] args) throws Exception {
        // Basic startup: java -cp ... CoordinatorServer [port] [kafkaBootstrap]
        DistributedConfig cfg = new DistributedConfig();
        CoordinatorServer srv = new CoordinatorServer(cfg);
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try { srv.stop(); } catch (Exception ignored) {}
        }));
        srv.start();
        srv.join();
    }
}
