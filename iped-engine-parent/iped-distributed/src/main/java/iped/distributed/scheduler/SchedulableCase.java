package iped.distributed.scheduler;

/**
 * Input to the {@link CaseScheduler}: one case competing for agent capacity of a given task type.
 *
 * @param caseId case identifier
 * @param priority scheduling priority class
 * @param pendingWork units of queued, not-yet-started work for this case and task type (e.g.
 *     consumer lag on the case's stage topic). A case with {@code pendingWork == 0} is never
 *     assigned slots.
 * @param inFlight units currently being processed for this case and task type; informational only —
 *     the scheduler never preempts in-flight work, it only distributes free slots.
 */
public record SchedulableCase(String caseId, CasePriority priority, int pendingWork, int inFlight) {
  public SchedulableCase {
    if (caseId == null) throw new IllegalArgumentException("caseId must not be null");
    if (priority == null) priority = CasePriority.NORMAL;
    if (pendingWork < 0) pendingWork = 0;
    if (inFlight < 0) inFlight = 0;
  }

  public static SchedulableCase of(String caseId, CasePriority priority, int pendingWork) {
    return new SchedulableCase(caseId, priority, pendingWork, 0);
  }
}
