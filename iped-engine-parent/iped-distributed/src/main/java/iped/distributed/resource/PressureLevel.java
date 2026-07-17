package iped.distributed.resource;

/**
 * Severity of an agent's local resource pressure, reported to the coordinator on each heartbeat and
 * used to decide whether the agent should receive new work.
 *
 * <ul>
 *   <li>{@link #NONE} — healthy; schedule normally.
 *   <li>{@link #SOFT} — approaching a limit; the agent keeps working but the signal is surfaced so
 *       operators (and a future weighted scheduler) can react.
 *   <li>{@link #HARD} — at/over a limit; the agent must receive <b>no new work</b>. The coordinator
 *       withholds its free slots from scheduling and the agent pauses its own intake until pressure
 *       clears. In-flight items are never preempted.
 * </ul>
 */
public enum PressureLevel {
  NONE,
  SOFT,
  HARD;

  /** True if this level is at least as severe as {@code other}. */
  public boolean atLeast(PressureLevel other) {
    return ordinal() >= other.ordinal();
  }

  /** Returns the more severe of two levels. */
  public static PressureLevel max(PressureLevel a, PressureLevel b) {
    return a.ordinal() >= b.ordinal() ? a : b;
  }
}
