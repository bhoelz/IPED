package iped.distributed.coordinator;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import iped.distributed.kafka.TopicProvisioner;
import iped.distributed.scheduler.CasePriority;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import lombok.extern.slf4j.Slf4j;

/**
 * Manages the lifecycle (start → processing → completion) of distributed cases.
 *
 * <p>On {@link #startCase}, pipeline Kafka topics are provisioned and the task-to-stage mapping is
 * stored. Agents query this mapping via {@link #getStageForTask(String, String)} to know which
 * topics to consume from / produce to.
 *
 * <h3>Durability and coordinator failover</h3>
 *
 * <p>The task pipeline order is the one piece of coordinator state that <b>cannot</b> be
 * reconstructed from Kafka — status events carry per-item stage numbers but not the authoritative
 * ordered task list. When constructed with a {@code stateDir}, this manager persists every case
 * definition to {@code cases.json} on {@link #startCase} and {@link #completeCase}, and reloads it
 * on construction. A restarted coordinator therefore recovers which cases exist and their pipeline
 * shape without re-issuing {@code /cases/start}. Completion <i>progress</i> is recovered separately
 * by replaying {@code iped.status} (see {@link CaseCompletionMonitor#recover}). Without a {@code
 * stateDir} the manager is purely in-memory (legacy behaviour).
 */
@Slf4j
public class CaseLifecycleManager {

  private final TopicProvisioner topicProvisioner;

  /** caseId → (taskType → stageNumber) */
  private final Map<String, Map<String, Integer>> caseTaskStageMap = new ConcurrentHashMap<>();

  /** caseId → ordered list of task types */
  private final Map<String, List<String>> caseTaskOrder = new ConcurrentHashMap<>();

  /** caseId → status */
  private final Map<String, CaseStatus> caseStatuses = new ConcurrentHashMap<>();

  /** Optional durable store for case definitions; null = in-memory only. */
  private final Path stateFile;

  private final ObjectMapper mapper = new ObjectMapper().registerModule(new JavaTimeModule());

  public CaseLifecycleManager(TopicProvisioner topicProvisioner) {
    this(topicProvisioner, null);
  }

  /**
   * @param topicProvisioner Kafka topic provisioner
   * @param stateDir directory in which to persist {@code cases.json} for crash recovery; {@code
   *     null} disables persistence
   */
  public CaseLifecycleManager(TopicProvisioner topicProvisioner, Path stateDir) {
    this.topicProvisioner = topicProvisioner;
    this.stateFile = stateDir != null ? stateDir.resolve("cases.json") : null;
    if (stateFile != null) loadState();
  }

  // -----------------------------------------------------------------------

  /**
   * Starts a new distributed case: provisions Kafka topics and stores the task pipeline order.
   *
   * @param caseId unique case identifier
   * @param orderedTaskNames task class simple names in execution order
   * @param partitions Kafka partitions per topic
   * @param replication Kafka replication factor
   */
  public void startCase(
      String caseId, List<String> orderedTaskNames, int partitions, short replication) {
    startCase(caseId, orderedTaskNames, partitions, replication, CasePriority.NORMAL);
  }

  /** As {@link #startCase(String, List, int, short)} but with an explicit scheduling priority. */
  public void startCase(
      String caseId,
      List<String> orderedTaskNames,
      int partitions,
      short replication,
      CasePriority priority) {
    log.info(
        "Starting case '{}' (priority={}) with {} tasks: {}",
        caseId,
        priority,
        orderedTaskNames.size(),
        orderedTaskNames);

    // Provision Kafka topics (idempotent — safe if a previous coordinator already created them)
    topicProvisioner.provisionCase(caseId, orderedTaskNames.size(), partitions, replication);

    registerCase(
        caseId,
        orderedTaskNames,
        CaseStatus.State.RUNNING,
        java.time.Instant.now(),
        null,
        priority);
    persistState();

    log.info("Case '{}' started successfully", caseId);
  }

  /**
   * Sets (re-prioritises) a case's scheduling priority. Affects only which case the <em>next</em>
   * freed agent slot pulls from — items already running are never preempted. Persists the change.
   *
   * @return true if the case exists and was updated
   */
  public boolean setCasePriority(String caseId, CasePriority priority) {
    CaseStatus status = caseStatuses.get(caseId);
    if (status == null) return false;
    status.priority = priority != null ? priority : CasePriority.NORMAL;
    persistState();
    log.info("Case '{}' priority set to {}", caseId, status.priority);
    return true;
  }

  /**
   * Registers a case definition in the in-memory maps without provisioning topics or persisting.
   * Shared by {@link #startCase} and {@link #loadState}.
   */
  private void registerCase(
      String caseId,
      List<String> orderedTaskNames,
      CaseStatus.State state,
      java.time.Instant startedAt,
      java.time.Instant completedAt,
      CasePriority priority) {
    Map<String, Integer> stageMap = new LinkedHashMap<>();
    for (int i = 0; i < orderedTaskNames.size(); i++) {
      stageMap.put(orderedTaskNames.get(i), i);
    }
    caseTaskStageMap.put(caseId, stageMap);
    caseTaskOrder.put(caseId, new ArrayList<>(orderedTaskNames));

    CaseStatus status = new CaseStatus();
    status.caseId = caseId;
    status.state = state;
    status.orderedTasks = new ArrayList<>(orderedTaskNames);
    status.startedAt = startedAt;
    status.completedAt = completedAt;
    status.priority = priority != null ? priority : CasePriority.NORMAL;
    caseStatuses.put(caseId, status);
  }

  /** Returns the stage number for a given task type in a case. */
  public int getStageForTask(String caseId, String taskType) {
    Map<String, Integer> map = caseTaskStageMap.get(caseId);
    if (map == null) throw new IllegalArgumentException("Unknown case: " + caseId);
    Integer stage = map.get(taskType);
    if (stage == null)
      throw new IllegalArgumentException(
          "Task '" + taskType + "' not found in case '" + caseId + "'");
    return stage;
  }

  /** Returns the ordered list of task types for a case. */
  public List<String> getTaskOrder(String caseId) {
    List<String> order = caseTaskOrder.get(caseId);
    if (order == null) throw new IllegalArgumentException("Unknown case: " + caseId);
    return Collections.unmodifiableList(order);
  }

  /** Returns the current status of a case. */
  public CaseStatus getStatus(String caseId) {
    return caseStatuses.getOrDefault(caseId, null);
  }

  /**
   * Pauses a running case — agents will stop receiving its topics on the next heartbeat. Idempotent
   * on already-paused cases. Returns {@code false} for unknown/completed cases.
   */
  public boolean pauseCase(String caseId) {
    CaseStatus status = caseStatuses.get(caseId);
    if (status == null
        || status.state == CaseStatus.State.COMPLETED
        || status.state == CaseStatus.State.FAILED) return false;
    status.state = CaseStatus.State.PAUSED;
    persistState();
    log.info("Case '{}' paused", caseId);
    return true;
  }

  /** Resumes a paused case. Returns {@code false} for unknown or non-paused cases. */
  public boolean resumeCase(String caseId) {
    CaseStatus status = caseStatuses.get(caseId);
    if (status == null || status.state != CaseStatus.State.PAUSED) return false;
    status.state = CaseStatus.State.RUNNING;
    persistState();
    log.info("Case '{}' resumed", caseId);
    return true;
  }

  /**
   * Merges {@code tags} into the case's metadata map (existing keys are overwritten). Returns
   * {@code false} for unknown cases.
   */
  public boolean updateTags(String caseId, Map<String, String> tags) {
    CaseStatus status = caseStatuses.get(caseId);
    if (status == null) return false;
    if (status.metadata == null) status.metadata = new ConcurrentHashMap<>();
    if (tags != null) status.metadata.putAll(tags);
    persistState();
    log.debug("Case '{}' metadata updated: {} key(s)", caseId, tags != null ? tags.size() : 0);
    return true;
  }

  /**
   * Removes a case from the lifecycle manager and deprovisions its Kafka topics. Idempotent —
   * returns {@code false} when the case does not exist.
   */
  public boolean deleteCase(String caseId) {
    CaseStatus status = caseStatuses.remove(caseId);
    if (status == null) return false;
    caseTaskStageMap.remove(caseId);
    caseTaskOrder.remove(caseId);
    if (topicProvisioner != null && status.orderedTasks != null) {
      try {
        topicProvisioner.deprovisionCase(caseId, status.orderedTasks.size());
      } catch (Exception e) {
        log.warn("Failed to deprovision topics for case '{}': {}", caseId, e.getMessage());
      }
    }
    persistState();
    log.info("Case '{}' deleted", caseId);
    return true;
  }

  /** Marks a case as completed. */
  public void completeCase(String caseId) {
    CaseStatus status = caseStatuses.get(caseId);
    if (status != null) {
      status.state = CaseStatus.State.COMPLETED;
      status.completedAt = java.time.Instant.now();
      persistState();
      log.info("Case '{}' marked as COMPLETED", caseId);
    }
  }

  /** Returns all known cases. */
  public Collection<CaseStatus> allCases() {
    return caseStatuses.values();
  }

  // ---- Durable state -----------------------------------------------------

  /**
   * Writes the current case definitions to {@code cases.json} (atomic replace). No-op when
   * persistence is disabled. Failures are logged but never propagate — losing the durable snapshot
   * must not break live case handling.
   */
  private synchronized void persistState() {
    if (stateFile == null) return;
    try {
      Files.createDirectories(stateFile.getParent());
      // writeValueAsBytes + Files.write ensures the handle is closed before
      // the rename — avoids Windows handle-still-open races on delete/rename.
      byte[] json =
          mapper
              .writerWithDefaultPrettyPrinter()
              .writeValueAsBytes(new ArrayList<>(caseStatuses.values()));
      Path tmp = stateFile.resolveSibling("cases.json.tmp");
      Files.write(tmp, json);
      Files.move(
          tmp,
          stateFile,
          java.nio.file.StandardCopyOption.REPLACE_EXISTING,
          java.nio.file.StandardCopyOption.ATOMIC_MOVE);
    } catch (IOException e) {
      log.warn("Could not persist case state to {}: {}", stateFile, e.getMessage());
    }
  }

  /**
   * Reloads case definitions from {@code cases.json} on construction. Rebuilds the task-order and
   * stage maps from each persisted {@link CaseStatus#orderedTasks}. A missing or unreadable file
   * leaves the manager empty (fresh start).
   */
  private void loadState() {
    if (stateFile == null || !Files.isReadable(stateFile)) return;
    try {
      CaseStatus[] saved = mapper.readValue(Files.readAllBytes(stateFile), CaseStatus[].class);
      for (CaseStatus s : saved) {
        if (s.caseId == null || s.orderedTasks == null) continue;
        registerCase(s.caseId, s.orderedTasks, s.state, s.startedAt, s.completedAt, s.priority);
      }
      log.info("Recovered {} case definition(s) from {}", saved.length, stateFile);
    } catch (IOException e) {
      log.warn("Could not load case state from {} — starting empty: {}", stateFile, e.getMessage());
    }
  }

  // ---- Nested DTO --------------------------------------------------------

  public static class CaseStatus {
    public enum State {
      RUNNING,
      PAUSED,
      COMPLETED,
      FAILED
    }

    public String caseId;
    public State state;
    public List<String> orderedTasks;
    public java.time.Instant startedAt;
    public java.time.Instant completedAt;
    public long totalDiscovered;
    public long totalCompleted;

    /** Scheduling priority; defaults to {@link CasePriority#NORMAL}. */
    public CasePriority priority = CasePriority.NORMAL;

    /** Arbitrary operator-supplied key-value metadata (e.g. investigator, reference number). */
    public Map<String, String> metadata;
  }
}
