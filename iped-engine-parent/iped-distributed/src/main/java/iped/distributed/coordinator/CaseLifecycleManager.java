package iped.distributed.coordinator;

import iped.distributed.kafka.TopicProvisioner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages the lifecycle (start → processing → completion) of distributed cases.
 *
 * <p>On {@link #startCase}, pipeline Kafka topics are provisioned and the
 * task-to-stage mapping is stored. Agents query this mapping via
 * {@link #getStageForTask(String, String)} to know which topics to consume from / produce to.
 */
public class CaseLifecycleManager {

    private static final Logger LOGGER = LoggerFactory.getLogger(CaseLifecycleManager.class);

    private final TopicProvisioner topicProvisioner;

    /** caseId → (taskType → stageNumber) */
    private final Map<String, Map<String, Integer>> caseTaskStageMap = new ConcurrentHashMap<>();

    /** caseId → ordered list of task types */
    private final Map<String, List<String>> caseTaskOrder = new ConcurrentHashMap<>();

    /** caseId → status */
    private final Map<String, CaseStatus> caseStatuses = new ConcurrentHashMap<>();

    public CaseLifecycleManager(TopicProvisioner topicProvisioner) {
        this.topicProvisioner = topicProvisioner;
    }

    // -----------------------------------------------------------------------

    /**
     * Starts a new distributed case: provisions Kafka topics and stores the
     * task pipeline order.
     *
     * @param caseId           unique case identifier
     * @param orderedTaskNames task class simple names in execution order
     * @param partitions       Kafka partitions per topic
     * @param replication      Kafka replication factor
     */
    public void startCase(String caseId, List<String> orderedTaskNames,
                           int partitions, short replication) {
        LOGGER.info("Starting case '{}' with {} tasks: {}", caseId,
                orderedTaskNames.size(), orderedTaskNames);

        // Provision Kafka topics
        topicProvisioner.provisionCase(caseId, orderedTaskNames.size(), partitions, replication);

        // Build taskType → stageNumber map
        Map<String, Integer> stageMap = new LinkedHashMap<>();
        for (int i = 0; i < orderedTaskNames.size(); i++) {
            stageMap.put(orderedTaskNames.get(i), i);
        }
        caseTaskStageMap.put(caseId, stageMap);
        caseTaskOrder.put(caseId, new ArrayList<>(orderedTaskNames));

        CaseStatus status = new CaseStatus();
        status.caseId         = caseId;
        status.state          = CaseStatus.State.RUNNING;
        status.orderedTasks   = new ArrayList<>(orderedTaskNames);
        status.startedAt      = java.time.Instant.now();
        caseStatuses.put(caseId, status);

        LOGGER.info("Case '{}' started successfully", caseId);
    }

    /** Returns the stage number for a given task type in a case. */
    public int getStageForTask(String caseId, String taskType) {
        Map<String, Integer> map = caseTaskStageMap.get(caseId);
        if (map == null) throw new IllegalArgumentException("Unknown case: " + caseId);
        Integer stage = map.get(taskType);
        if (stage == null) throw new IllegalArgumentException(
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

    /** Marks a case as completed. */
    public void completeCase(String caseId) {
        CaseStatus status = caseStatuses.get(caseId);
        if (status != null) {
            status.state       = CaseStatus.State.COMPLETED;
            status.completedAt = java.time.Instant.now();
            LOGGER.info("Case '{}' marked as COMPLETED", caseId);
        }
    }

    /** Returns all known cases. */
    public Collection<CaseStatus> allCases() {
        return caseStatuses.values();
    }

    // ---- Nested DTO --------------------------------------------------------

    public static class CaseStatus {
        public enum State { RUNNING, COMPLETED, FAILED }

        public String           caseId;
        public State            state;
        public List<String>     orderedTasks;
        public java.time.Instant startedAt;
        public java.time.Instant completedAt;
        public long             totalDiscovered;
        public long             totalCompleted;
    }
}
