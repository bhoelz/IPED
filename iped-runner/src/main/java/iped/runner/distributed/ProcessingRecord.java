package iped.runner.distributed;

import java.time.Instant;

/**
 * Immutable record of the terminal outcome of processing one item through one pipeline task.
 *
 * <p>Created from {@link StatusEvent} when the event type is COMPLETED, ERROR, or TIMEOUT. All
 * other event types (DISCOVERED, STARTED, SUBITEM_DISCOVERED, CASE_COMPLETED) return {@code null}
 * from {@link #from(StatusEvent, String)}.
 */
public record ProcessingRecord(
    String itemUuid,
    String caseId,
    String taskType,
    int pipelineStage,
    String agentId,
    Instant processedAt,
    long durationMs,
    Outcome outcome,
    String errorMessage) {
  public enum Outcome {
    COMPLETED,
    ERROR,
    TIMEOUT
  }

  /**
   * Attempts to build a {@code ProcessingRecord} from a {@link StatusEvent}.
   *
   * @param event the status event to convert
   * @param agentId the publishing agent's identity (stamped by {@code ItemStatusProducer})
   * @return a new record, or {@code null} when the event type is not terminal
   */
  public static ProcessingRecord from(StatusEvent event, String agentId) {
    if (event == null || event.getType() == null) return null;
    Outcome outcome =
        switch (event.getType()) {
          case COMPLETED -> Outcome.COMPLETED;
          case ERROR -> Outcome.ERROR;
          case TIMEOUT -> Outcome.TIMEOUT;
          default -> null;
        };
    if (outcome == null) return null;

    return new ProcessingRecord(
        event.getItemUuid(),
        event.getCaseId(),
        event.getTaskType(),
        event.getPipelineStage(),
        agentId,
        event.getTimestamp() != null ? event.getTimestamp() : Instant.now(),
        event.getDurationMs(),
        outcome,
        event.getErrorMessage());
  }
}
