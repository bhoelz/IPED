package iped.distributed.scheduler;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;

/**
 * Decides how a pool of free agent slots (of one task type) is distributed across the active cases
 * competing for them.
 *
 * <h2>Policy: strict priority across classes, round-robin within a class</h2>
 *
 * <ol>
 *   <li>Cases with no pending work are ignored.
 *   <li>Higher {@link CasePriority} classes are served <b>completely</b> before lower ones: all
 *       pending URGENT work that fits is assigned before any HIGH work, and so on. This is what
 *       lets an urgent case "preempt the queue order" of normal cases.
 *   <li>Within one priority class, free slots are handed out <b>round-robin</b> across the cases
 *       (one at a time, cycling), so equal-priority cases share capacity fairly and no single case
 *       monopolises the pool.
 *   <li>A case is never assigned more slots than it has pending work.
 *   <li>Only free slots are scheduled — <b>in-flight items are never preempted</b> ("not running
 *       segments"). Re-prioritising a case changes which case the <i>next</i> freed slot pulls
 *       from; items already running finish normally.
 * </ol>
 *
 * <h2>Starvation note</h2>
 *
 * <p>Strict priority means a steady stream of URGENT work can starve LOW/NORMAL cases — this is
 * intentional ("preempt the queue order"). Operators raise/lower a case's priority to control this;
 * the policy is deterministic given its inputs.
 *
 * <p>The scheduler is stateless and thread-safe. It is invoked per task type: the coordinator
 * supplies the cases that have work for that task type and the number of free slots reported by the
 * {@code AgentRegistry} for that type.
 */
@Slf4j
public class CaseScheduler {

  /**
   * Assigns {@code freeSlots} across the given cases per the priority policy.
   *
   * @param cases candidate cases (any with {@code pendingWork == 0} are skipped)
   * @param freeSlots number of free agent slots available for this task type
   * @return per-case slot assignments, ordered by priority (highest first) then caseId; only cases
   *     that received at least one slot are included. Empty when there is no free capacity or no
   *     pending work.
   */
  public List<SlotAssignment> schedule(Collection<SchedulableCase> cases, int freeSlots) {
    if (freeSlots <= 0 || cases == null || cases.isEmpty()) return List.of();

    // Eligible cases sorted by priority (desc) then caseId for deterministic order.
    List<SchedulableCase> eligible =
        cases.stream()
            .filter(c -> c != null && c.pendingWork() > 0)
            .sorted(
                Comparator.comparingInt((SchedulableCase c) -> c.priority().weight())
                    .reversed()
                    .thenComparing(SchedulableCase::caseId))
            .toList();
    if (eligible.isEmpty()) return List.of();

    Map<String, Integer> remainingDemand = new LinkedHashMap<>();
    Map<String, Integer> assigned = new LinkedHashMap<>();
    for (SchedulableCase c : eligible) remainingDemand.put(c.caseId(), c.pendingWork());

    int remaining = freeSlots;
    for (List<SchedulableCase> group : groupByPriority(eligible)) {
      if (remaining <= 0) break;
      // Round-robin within the priority class until slots or demand are exhausted.
      boolean progress = true;
      while (remaining > 0 && progress) {
        progress = false;
        for (SchedulableCase c : group) {
          if (remaining <= 0) break;
          int d = remainingDemand.get(c.caseId());
          if (d > 0) {
            assigned.merge(c.caseId(), 1, Integer::sum);
            remainingDemand.put(c.caseId(), d - 1);
            remaining--;
            progress = true;
          }
        }
      }
    }

    List<SlotAssignment> out = new ArrayList<>();
    for (SchedulableCase c : eligible) {
      Integer a = assigned.get(c.caseId());
      if (a != null && a > 0) out.add(new SlotAssignment(c.caseId(), c.priority(), a));
    }
    if (log.isDebugEnabled()) {
      log.debug(
          "Scheduled {} of {} free slot(s) across {} case(s)",
          freeSlots - remaining,
          freeSlots,
          out.size());
    }
    return out;
  }

  /** Splits the priority-sorted list into contiguous groups of equal priority. */
  private static List<List<SchedulableCase>> groupByPriority(List<SchedulableCase> sorted) {
    List<List<SchedulableCase>> groups = new ArrayList<>();
    CasePriority currentPriority = null;
    List<SchedulableCase> current = null;
    for (SchedulableCase c : sorted) {
      if (c.priority() != currentPriority) {
        current = new ArrayList<>();
        groups.add(current);
        currentPriority = c.priority();
      }
      current.add(c);
    }
    return groups;
  }
}
