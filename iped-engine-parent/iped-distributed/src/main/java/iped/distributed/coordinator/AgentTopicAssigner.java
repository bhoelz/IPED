package iped.distributed.coordinator;

import iped.distributed.kafka.TopicProvisioner;
import iped.distributed.scheduler.CasePriority;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Computes which Kafka input-stage topics a registered agent should subscribe to,
 * given the currently active case set and the agent's task type.
 *
 * <p>Used by the coordinator heartbeat endpoint to return steering directives to agents.
 * Only {@code RUNNING} cases that contain the agent's task type in their pipeline are
 * included.  The returned list is sorted lexicographically so agents can compare it
 * cheaply with {@code List.equals}.
 *
 * <p>When a {@code maxSubscribedCases} cap is configured, the list is truncated to the
 * highest-priority running cases: cases are first ordered by {@link CasePriority} (descending)
 * then lexicographically by case ID (ascending) for determinism within the same priority.
 * The result is re-sorted lexicographically after capping so the consumer-side equals
 * check remains reliable.
 *
 * <p>This class is stateless and therefore thread-safe.
 */
public class AgentTopicAssigner {

    private final AgentRegistry registry;
    private final CaseLifecycleManager lifecycle;
    private final int maxSubscribedCases;

    /** No cap on subscribed cases. */
    public AgentTopicAssigner(AgentRegistry registry, CaseLifecycleManager lifecycle) {
        this(registry, lifecycle, 0);
    }

    /**
     * @param maxSubscribedCases maximum topics to return per agent; {@code 0} = unlimited
     */
    public AgentTopicAssigner(AgentRegistry registry, CaseLifecycleManager lifecycle,
                              int maxSubscribedCases) {
        this.registry           = registry;
        this.lifecycle          = lifecycle;
        this.maxSubscribedCases = maxSubscribedCases;
    }

    /**
     * Returns the sorted list of Kafka input-stage topics the given agent should
     * currently subscribe to.
     *
     * @param agentId the agent's unique identifier (as registered via heartbeat)
     * @return sorted topic list; empty when the agent is unknown or no work is pending
     */
    public List<String> topicsForAgent(String agentId) {
        AgentRegistration reg = registry.getAgent(agentId);
        if (reg == null) return List.of();
        String taskType = reg.getTaskType();

        Stream<CaseLifecycleManager.CaseStatus> runningCases = lifecycle.allCases().stream()
                .filter(c -> c.state == CaseLifecycleManager.CaseStatus.State.RUNNING) // excludes PAUSED
                .filter(c -> c.orderedTasks != null && c.orderedTasks.contains(taskType));

        // Apply priority-based cap before mapping to topics.
        // Sort by weight() descending (higher weight = higher priority = served first),
        // then by caseId ascending for determinism within the same priority class.
        if (maxSubscribedCases > 0) {
            runningCases = runningCases
                    .sorted(Comparator
                            .<CaseLifecycleManager.CaseStatus>comparingInt(
                                    c -> -(c.priority != null ? c.priority.weight() : CasePriority.NORMAL.weight()))
                            .thenComparing(c -> c.caseId))
                    .limit(maxSubscribedCases);
        }

        return runningCases
                .map(c -> {
                    try {
                        int stage = lifecycle.getStageForTask(c.caseId, taskType);
                        return TopicProvisioner.stageTopic(c.caseId, stage);
                    } catch (IllegalArgumentException ignored) {
                        return null;
                    }
                })
                .filter(t -> t != null)
                .sorted()
                .collect(Collectors.toList());
    }
}
