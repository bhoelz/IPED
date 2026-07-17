package iped.distributed.scheduler;

/**
 * Output of the {@link CaseScheduler}: how many free agent slots of a task type should next be
 * directed at a given case. Assignments target <b>free</b> capacity only and never reduce a case's
 * in-flight work.
 *
 * @param caseId the case to pull work from
 * @param priority the case's priority class (carried through for observability)
 * @param slots number of free slots assigned to this case this round (≥ 1)
 */
public record SlotAssignment(String caseId, CasePriority priority, int slots) {
  public SlotAssignment {
    if (slots <= 0) throw new IllegalArgumentException("slots must be > 0");
  }
}
