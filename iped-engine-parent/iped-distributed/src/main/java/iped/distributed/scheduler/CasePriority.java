package iped.distributed.scheduler;

/**
 * Priority class of a distributed case, governing the order in which its pending work
 * is assigned to free agent capacity by the {@link CaseScheduler}.
 *
 * <p>Higher {@link #weight()} is served first.  Scheduling is <b>strict</b> across
 * classes: all pending work of a higher class is assigned before any work of a lower
 * class (an urgent case "preempts the queue order" of normal cases).  Within the same
 * class, free slots are shared round-robin so equal-priority cases do not starve each
 * other.  Running items are never preempted — only free slots are scheduled.
 */
public enum CasePriority {

    /** Time-critical case (e.g. an active investigation with a legal deadline). */
    URGENT(1000),
    /** Elevated priority above the default. */
    HIGH(100),
    /** Default priority. */
    NORMAL(10),
    /** Background / best-effort; yields to everything else. */
    LOW(1);

    private final int weight;

    CasePriority(int weight) {
        this.weight = weight;
    }

    /** Relative scheduling weight; higher is served first. */
    public int weight() {
        return weight;
    }

    /** Returns {@link #NORMAL} for a null/blank name, else the matching value (case-insensitive). */
    public static CasePriority parseOrDefault(String name) {
        if (name == null || name.isBlank()) return NORMAL;
        try {
            return CasePriority.valueOf(name.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return NORMAL;
        }
    }
}
