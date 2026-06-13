package iped.distributed.dualrun;

/**
 * Overall outcome of a dual-run comparison for one case.
 *
 * <ul>
 *   <li>{@link #INCOMPLETE} — at least one side has not yet finished; the report
 *       is a partial view, not a final verdict.</li>
 *   <li>{@link #MATCH} — both paths discovered and processed the same set of items
 *       with identical observable attributes.  Safe to cut over to the distributed
 *       path.</li>
 *   <li>{@link #MISMATCH} — discrepancies found (missing items, extra items, or
 *       attribute differences).  Investigate before cutting over.</li>
 * </ul>
 */
public enum DualRunVerdict {
    INCOMPLETE,
    MATCH,
    MISMATCH
}
