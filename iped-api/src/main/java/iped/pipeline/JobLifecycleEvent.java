package iped.pipeline;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Immutable event fired at each transition of a processing job's lifecycle.
 *
 * <p>Consumers (e.g., the distributed coordinator, the runner dashboard) register an {@link
 * IJobLifecycleListener} to receive these events without depending on Kafka or engine internals.
 */
public final class JobLifecycleEvent {

  private final UUID caseId;
  private final JobPhase phase;
  private final Instant timestamp;
  private final String detail;
  private final Throwable cause;

  private JobLifecycleEvent(
      UUID caseId, JobPhase phase, Instant timestamp, String detail, Throwable cause) {
    this.caseId = Objects.requireNonNull(caseId, "caseId");
    this.phase = Objects.requireNonNull(phase, "phase");
    this.timestamp = timestamp != null ? timestamp : Instant.now();
    this.detail = detail;
    this.cause = cause;
  }

  /**
   * @return the case/job identifier
   */
  public UUID caseId() {
    return caseId;
  }

  /**
   * @return the lifecycle phase that just occurred
   */
  public JobPhase phase() {
    return phase;
  }

  /**
   * @return when this event occurred
   */
  public Instant timestamp() {
    return timestamp;
  }

  /**
   * @return optional human-readable detail message, or {@code null}
   */
  public String detail() {
    return detail;
  }

  /**
   * @return the cause if {@link JobPhase#FAILED}, otherwise {@code null}
   */
  public Throwable cause() {
    return cause;
  }

  /** Creates an event with the current time and no detail. */
  public static JobLifecycleEvent of(UUID caseId, JobPhase phase) {
    return new JobLifecycleEvent(caseId, phase, Instant.now(), null, null);
  }

  /** Creates an event with a detail message. */
  public static JobLifecycleEvent of(UUID caseId, JobPhase phase, String detail) {
    return new JobLifecycleEvent(caseId, phase, Instant.now(), detail, null);
  }

  /** Creates a {@link JobPhase#FAILED} event with a cause. */
  public static JobLifecycleEvent failed(UUID caseId, Throwable cause) {
    return new JobLifecycleEvent(
        caseId, JobPhase.FAILED, Instant.now(), cause != null ? cause.getMessage() : null, cause);
  }

  @Override
  public String toString() {
    return "JobLifecycleEvent{caseId="
        + caseId
        + ", phase="
        + phase
        + ", timestamp="
        + timestamp
        + (detail != null ? ", detail='" + detail + "'" : "")
        + '}';
  }
}
