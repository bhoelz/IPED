package iped.distributed.coordinator;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * In-memory ring buffer of recent coordinator lifecycle events.
 *
 * <p>Events are appended via {@link #append} and read via {@link #latest}. When the buffer is full
 * the oldest entry is silently overwritten. Thread-safe.
 *
 * <p>Exposed via {@code GET /api/v1/events?limit=N} on the coordinator.
 */
public class CoordinatorEventLog {

  /** Default capacity (number of events retained). */
  public static final int DEFAULT_CAPACITY = 500;

  // ---- Event types --------------------------------------------------------

  public enum EventType {
    CASE_STARTED,
    CASE_PAUSED,
    CASE_RESUMED,
    CASE_COMPLETED,
    CASE_DELETED,
    CASE_STALLED,
    AGENT_REGISTERED,
    AGENT_EXPIRED,
    AGENT_UNREGISTERED,
    AGENT_PRESSURE_HARD,
    AGENT_PRESSURE_CLEARED,
    CONFIG_VALIDATION_FAILED,
    DLQ_AUTO_RETRY,
  }

  // ---- Event DTO ----------------------------------------------------------

  public record CoordinatorEvent(
      EventType type, String caseId, String agentId, String message, Instant timestamp) {
    static CoordinatorEvent of(EventType type, String caseId, String agentId, String msg) {
      return new CoordinatorEvent(type, caseId, agentId, msg, Instant.now());
    }

    public static CoordinatorEvent caseEvent(EventType type, String caseId, String msg) {
      return of(type, caseId, null, msg);
    }

    public static CoordinatorEvent agentEvent(EventType type, String agentId, String msg) {
      return of(type, null, agentId, msg);
    }

    public static CoordinatorEvent generic(EventType type, String msg) {
      return of(type, null, null, msg);
    }
  }

  // ---- Ring buffer --------------------------------------------------------

  private final int capacity;
  private final CoordinatorEvent[] ring;
  private int head = 0; // next write position (mod capacity)
  private int size = 0; // current fill level

  public CoordinatorEventLog() {
    this(DEFAULT_CAPACITY);
  }

  public CoordinatorEventLog(int capacity) {
    if (capacity < 1) throw new IllegalArgumentException("capacity must be >= 1");
    this.capacity = capacity;
    this.ring = new CoordinatorEvent[capacity];
  }

  /** Appends an event, overwriting the oldest entry when full. */
  public synchronized void append(CoordinatorEvent event) {
    ring[head] = event;
    head = (head + 1) % capacity;
    if (size < capacity) size++;
  }

  /**
   * Returns the most recent {@code n} events, oldest-first. If fewer than {@code n} events have
   * been recorded, all recorded events are returned.
   */
  public synchronized List<CoordinatorEvent> latest(int n) {
    if (n <= 0 || size == 0) return List.of();
    int count = Math.min(n, size);
    List<CoordinatorEvent> result = new ArrayList<>(count);
    // The oldest of the "count" entries is at position (head - size + (size - count)) mod capacity
    int start = (head - count + capacity * 2) % capacity;
    for (int i = 0; i < count; i++) {
      result.add(ring[(start + i) % capacity]);
    }
    return result;
  }

  /** Total number of events currently retained (never exceeds capacity). */
  public synchronized int size() {
    return size;
  }
}
