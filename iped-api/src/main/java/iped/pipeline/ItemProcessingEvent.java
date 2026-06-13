package iped.pipeline;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Immutable event recording the completion (or failure) of a single item's
 * processing through the task pipeline.
 *
 * <p>These events are the item-level counterpart of {@link JobLifecycleEvent}.
 * They are used by the distributed coordinator to track progress and drive
 * completion detection without importing engine or Kafka types.
 */
public final class ItemProcessingEvent {

    /** Outcome of item processing. */
    public enum Outcome { SUCCESS, FAILURE, SKIPPED }

    private final UUID caseId;
    private final String itemUuid;
    private final String taskName;
    private final Outcome outcome;
    private final Instant timestamp;
    private final String errorMessage;

    private ItemProcessingEvent(UUID caseId, String itemUuid, String taskName,
                                Outcome outcome, Instant timestamp, String errorMessage) {
        this.caseId       = Objects.requireNonNull(caseId,   "caseId");
        this.itemUuid     = Objects.requireNonNull(itemUuid, "itemUuid");
        this.taskName     = taskName;
        this.outcome      = Objects.requireNonNull(outcome,  "outcome");
        this.timestamp    = timestamp != null ? timestamp : Instant.now();
        this.errorMessage = errorMessage;
    }

    /** @return the case this item belongs to */
    public UUID caseId() { return caseId; }

    /** @return stable item UUID (content hash or assigned UUID, not Lucene doc id) */
    public String itemUuid() { return itemUuid; }

    /** @return simple class name of the task that last processed this item */
    public String taskName() { return taskName; }

    /** @return processing outcome */
    public Outcome outcome() { return outcome; }

    /** @return when this event was emitted */
    public Instant timestamp() { return timestamp; }

    /** @return error message if {@link Outcome#FAILURE}, otherwise {@code null} */
    public String errorMessage() { return errorMessage; }

    /** Creates a success event. */
    public static ItemProcessingEvent success(UUID caseId, String itemUuid, String taskName) {
        return new ItemProcessingEvent(caseId, itemUuid, taskName,
                Outcome.SUCCESS, Instant.now(), null);
    }

    /** Creates a failure event. */
    public static ItemProcessingEvent failure(UUID caseId, String itemUuid,
                                              String taskName, String errorMessage) {
        return new ItemProcessingEvent(caseId, itemUuid, taskName,
                Outcome.FAILURE, Instant.now(), errorMessage);
    }

    /** Creates a skipped event (item intentionally excluded from processing). */
    public static ItemProcessingEvent skipped(UUID caseId, String itemUuid, String taskName) {
        return new ItemProcessingEvent(caseId, itemUuid, taskName,
                Outcome.SKIPPED, Instant.now(), null);
    }

    @Override
    public String toString() {
        return "ItemProcessingEvent{caseId=" + caseId + ", itemUuid='" + itemUuid
                + "', task='" + taskName + "', outcome=" + outcome
                + ", timestamp=" + timestamp + '}';
    }
}
