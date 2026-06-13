package iped.distributed.coordinator;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.ArrayList;
import java.util.List;

/**
 * Response body returned by the coordinator heartbeat endpoint.
 *
 * <p>Carries the list of Kafka input-stage topics the agent should subscribe to
 * for its task type, based on the set of currently active cases.  The agent
 * compares this list to its current subscription on every heartbeat and calls
 * {@code consumer.subscribe()} if the list has changed — closing the loop on the
 * {@link CaseScheduler} plan without requiring a separate steering channel.
 *
 * <p>Schema: {@code @JsonIgnoreProperties(ignoreUnknown = true)} — forward-compatible
 * so that new coordinator fields (e.g. priority hints) can be added without breaking
 * agents running older code.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class HeartbeatResponse {

    /**
     * Kafka topics this agent should currently be subscribed to, sorted lexicographically.
     * Empty list means no active cases → agent should unsubscribe and idle.
     */
    private List<String> subscribedTopics = new ArrayList<>();

    public HeartbeatResponse() {}

    public HeartbeatResponse(List<String> subscribedTopics) {
        this.subscribedTopics = subscribedTopics != null
                ? new ArrayList<>(subscribedTopics)
                : new ArrayList<>();
    }

    public List<String> getSubscribedTopics()             { return subscribedTopics; }
    public void         setSubscribedTopics(List<String> v) {
        this.subscribedTopics = v != null ? v : new ArrayList<>();
    }
}
