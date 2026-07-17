package iped.engine.webapi;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.Consumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * In-memory store for async jobs submitted via {@link JobsV2}.
 *
 * <p>Jobs are keyed by a UUID string. Each job has a {@link JobEntry} that carries the current
 * status, progress counter, and a list of SSE subscriber queues so {@link JobsSseV2} can push
 * events without polling.
 *
 * <p>Completed/failed jobs are retained for {@value #RETENTION_SECONDS} seconds after reaching a
 * terminal state to allow callers to collect the result before GC. A daemon thread runs cleanup
 * every minute.
 */
public final class JobRegistry {

  private static final Logger LOG = LoggerFactory.getLogger(JobRegistry.class);
  private static final long RETENTION_SECONDS = 300;

  private static final ConcurrentHashMap<String, JobEntry> JOBS = new ConcurrentHashMap<>();

  private static final ScheduledExecutorService CLEANER =
      Executors.newSingleThreadScheduledExecutor(
          r -> {
            Thread t = new Thread(r, "job-registry-cleaner");
            t.setDaemon(true);
            return t;
          });

  static {
    CLEANER.scheduleAtFixedRate(JobRegistry::expire, 60, 60, TimeUnit.SECONDS);
  }

  private JobRegistry() {}

  // ── Public API ────────────────────────────────────────────────────────────

  public static JobEntry create(String type, Map<String, Object> params) {
    String id = UUID.randomUUID().toString();
    var entry = new JobEntry(id, type, params, Instant.now());
    JOBS.put(id, entry);
    LOG.info("Job created id={} type={}", id, type);
    return entry;
  }

  public static Optional<JobEntry> get(String id) {
    return Optional.ofNullable(JOBS.get(id));
  }

  public static List<JobEntry> all() {
    return JOBS.values().stream().sorted(Comparator.comparing(e -> e.createdAt)).toList();
  }

  static void remove(String id) {
    JOBS.remove(id);
  }

  // ── Expiry ────────────────────────────────────────────────────────────────

  private static void expire() {
    Instant cutoff = Instant.now().minusSeconds(RETENTION_SECONDS);
    JOBS.values()
        .removeIf(e -> e.isTerminal() && e.terminatedAt != null && e.terminatedAt.isBefore(cutoff));
  }

  // ── Job entry ─────────────────────────────────────────────────────────────

  public static final class JobEntry {
    public final String id;
    public final String type;
    public final Map<String, Object> params;
    public final Instant createdAt;

    public volatile String status = "pending"; // pending | running | completed | failed | cancelled
    public volatile int progress = 0; // 0–100
    public volatile String message = "";
    public volatile Instant terminatedAt = null;

    private final List<Consumer<JobEvent>> subscribers = new CopyOnWriteArrayList<>();

    private JobEntry(String id, String type, Map<String, Object> params, Instant createdAt) {
      this.id = id;
      this.type = type;
      this.params = Map.copyOf(params);
      this.createdAt = createdAt;
    }

    public boolean isTerminal() {
      return "completed".equals(status) || "failed".equals(status) || "cancelled".equals(status);
    }

    public void start() {
      status = "running";
      emit(new JobEvent("started", progress, message));
    }

    public void progress(int pct, String msg) {
      progress = pct;
      message = msg;
      emit(new JobEvent("progress", pct, msg));
    }

    public void complete(String msg) {
      status = "completed";
      progress = 100;
      message = msg;
      terminatedAt = Instant.now();
      emit(new JobEvent("completed", 100, msg));
      subscribers.clear();
      LOG.info("Job {} completed", id);
    }

    public void fail(String msg) {
      status = "failed";
      message = msg;
      terminatedAt = Instant.now();
      emit(new JobEvent("failed", progress, msg));
      subscribers.clear();
      LOG.warn("Job {} failed: {}", id, msg);
    }

    public void cancel() {
      status = "cancelled";
      terminatedAt = Instant.now();
      emit(new JobEvent("cancelled", progress, "Job cancelled"));
      subscribers.clear();
    }

    /** Subscribe to live events. The consumer is called on the job's worker thread. */
    public void subscribe(Consumer<JobEvent> consumer) {
      subscribers.add(consumer);
      // Immediately deliver current state so the subscriber is up-to-date.
      consumer.accept(new JobEvent(status, progress, message));
    }

    public void unsubscribe(Consumer<JobEvent> consumer) {
      subscribers.remove(consumer);
    }

    private void emit(JobEvent e) {
      subscribers.forEach(
          s -> {
            try {
              s.accept(e);
            } catch (Exception ignored) {
            }
          });
    }

    public Map<String, Object> toMap() {
      var m = new LinkedHashMap<String, Object>();
      m.put("id", id);
      m.put("type", type);
      m.put("status", status);
      m.put("progress", progress);
      m.put("message", message);
      m.put("createdAt", createdAt.toString());
      if (terminatedAt != null) m.put("terminatedAt", terminatedAt.toString());
      m.put("params", params);
      return m;
    }
  }

  /** Lightweight SSE payload for a job lifecycle transition. */
  public record JobEvent(String event, int progress, String message) {}
}
