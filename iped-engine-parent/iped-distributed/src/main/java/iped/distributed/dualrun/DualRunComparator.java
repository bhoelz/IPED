package iped.distributed.dualrun;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Stateless comparator that produces a {@link DualRunReport} from two item maps.
 *
 * <p>Both maps are keyed by the canonical item <b>path</b> (the matching key between the
 * distributed and reference paths). The distributed side is built from {@link ItemStatusEvent}
 * DISCOVERED events; the reference side is submitted by the operator from the monolithic run's
 * output.
 *
 * <p>Comparison rules:
 *
 * <ol>
 *   <li><b>Coverage</b>: items in reference but absent from distributed → {@link
 *       DualRunReport#missingFromDistributed}; items in distributed but absent from reference →
 *       {@link DualRunReport#extraInDistributed}.
 *   <li><b>Attribute agreement</b>: for items present in both paths, {@code mediaType} and {@code
 *       lengthBytes} are compared when <em>both</em> sides have a non-null value. If only one side
 *       has the field it is treated as unknown (no mismatch).
 *   <li><b>Verdict</b>: {@link DualRunVerdict#INCOMPLETE} if either side is not finished; {@link
 *       DualRunVerdict#MATCH} if all three discrepancy lists are empty; {@link
 *       DualRunVerdict#MISMATCH} otherwise.
 * </ol>
 */
public final class DualRunComparator {

  private DualRunComparator() {}

  /**
   * Produce a report comparing {@code distributedByPath} against {@code referenceByPath}.
   *
   * @param caseId identifier of the case being compared
   * @param distributedByPath items accumulated from the distributed pipeline, keyed by path
   * @param referenceByPath items from the reference (monolithic) run, keyed by path
   * @param distributedComplete true if the distributed pipeline has finished all items
   * @param referenceSubmitted true if the reference item set has been provided
   */
  public static DualRunReport compare(
      String caseId,
      Map<String, ItemSummary> distributedByPath,
      Map<String, ItemSummary> referenceByPath,
      boolean distributedComplete,
      boolean referenceSubmitted) {

    // Coverage: paths in reference but not in distributed
    List<String> allMissing =
        referenceByPath.keySet().stream()
            .filter(p -> !distributedByPath.containsKey(p))
            .sorted()
            .collect(Collectors.toList());

    // Coverage: paths in distributed but not in reference
    List<String> allExtra =
        distributedByPath.keySet().stream()
            .filter(p -> !referenceByPath.containsKey(p))
            .sorted()
            .collect(Collectors.toList());

    // Attribute comparison for matched paths
    List<AttributeMismatch> allMismatches = new ArrayList<>();
    for (Map.Entry<String, ItemSummary> entry : distributedByPath.entrySet()) {
      String path = entry.getKey();
      ItemSummary dist = entry.getValue();
      ItemSummary ref = referenceByPath.get(path);
      if (ref == null) continue;

      if (dist.mediaType() != null
          && ref.mediaType() != null
          && !dist.mediaType().equals(ref.mediaType())) {
        allMismatches.add(
            new AttributeMismatch(path, "mediaType", dist.mediaType(), ref.mediaType()));
      }
      if (dist.lengthBytes() != null
          && ref.lengthBytes() != null
          && !dist.lengthBytes().equals(ref.lengthBytes())) {
        allMismatches.add(
            new AttributeMismatch(
                path,
                "lengthBytes",
                String.valueOf(dist.lengthBytes()),
                String.valueOf(ref.lengthBytes())));
      }
    }

    // Determine verdict
    DualRunVerdict verdict;
    if (!referenceSubmitted || !distributedComplete) {
      verdict = DualRunVerdict.INCOMPLETE;
    } else if (allMissing.isEmpty() && allExtra.isEmpty() && allMismatches.isEmpty()) {
      verdict = DualRunVerdict.MATCH;
    } else {
      verdict = DualRunVerdict.MISMATCH;
    }

    // Cap lists for safe API responses
    int cap = DualRunReport.MAX_DISCREPANCY_LIST;
    return new DualRunReport(
        caseId,
        verdict,
        distributedByPath.size(),
        referenceByPath.size(),
        allMissing.size() > cap ? allMissing.subList(0, cap) : allMissing,
        allMissing.size(),
        allExtra.size() > cap ? allExtra.subList(0, cap) : allExtra,
        allExtra.size(),
        allMismatches.size() > cap ? allMismatches.subList(0, cap) : allMismatches,
        allMismatches.size(),
        distributedComplete,
        referenceSubmitted,
        Instant.now());
  }
}
