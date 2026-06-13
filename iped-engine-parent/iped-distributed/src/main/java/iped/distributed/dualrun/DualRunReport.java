package iped.distributed.dualrun;

import java.time.Instant;
import java.util.List;

/**
 * Snapshot result of comparing one case between the distributed and reference (monolithic) paths.
 *
 * <p>Produced by {@link DualRunComparator#compare}.  Serialisable to JSON by Jackson.
 *
 * <p>The discrepancy lists ({@link #missingFromDistributed}, {@link #extraInDistributed},
 * {@link #attributeMismatches}) are capped at {@link #MAX_DISCREPANCY_LIST} entries each
 * to keep API responses bounded; {@link #totalMissing} / {@link #totalExtra} /
 * {@link #totalAttributeMismatches} hold the true counts.
 */
public record DualRunReport(
        String caseId,
        DualRunVerdict verdict,
        int distributedItemCount,
        int referenceItemCount,
        List<String> missingFromDistributed,
        int totalMissing,
        List<String> extraInDistributed,
        int totalExtra,
        List<AttributeMismatch> attributeMismatches,
        int totalAttributeMismatches,
        boolean distributedComplete,
        boolean referenceSubmitted,
        Instant generatedAt
) {

    /** Maximum number of entries in each discrepancy list returned in the report. */
    public static final int MAX_DISCREPANCY_LIST = 500;

    /** True only when all three lists are empty and both sides have finished. */
    public boolean isPerfectMatch() {
        return verdict == DualRunVerdict.MATCH;
    }

    /** Short human-readable summary suitable for logging or display. */
    public String summary() {
        return switch (verdict) {
            case INCOMPLETE -> String.format(
                    "INCOMPLETE — dist=%d items (complete=%b), ref=%s",
                    distributedItemCount, distributedComplete,
                    referenceSubmitted ? referenceItemCount + " items" : "not submitted");
            case MATCH -> String.format("MATCH — %d items agreed", distributedItemCount);
            case MISMATCH -> {
                StringBuilder sb = new StringBuilder("MISMATCH");
                if (totalMissing > 0)
                    sb.append(" missing=").append(totalMissing);
                if (totalExtra > 0)
                    sb.append(" extra=").append(totalExtra);
                if (totalAttributeMismatches > 0)
                    sb.append(" attr-diff=").append(totalAttributeMismatches);
                sb.append(" (dist=").append(distributedItemCount)
                  .append(" ref=").append(referenceItemCount).append(')');
                yield sb.toString();
            }
        };
    }
}
