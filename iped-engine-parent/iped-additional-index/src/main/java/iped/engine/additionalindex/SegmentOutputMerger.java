package iped.engine.additionalindex;

import iped.datasource.AdditionalStoreSegment;
import java.util.LinkedHashMap;
import java.util.List;

/** Deterministic merge strategy for segment output, with later segments winning per key. */
public final class SegmentOutputMerger {
  public AdditionalStoreSegment merge(List<AdditionalStoreSegment> segments) {
    if (segments == null || segments.isEmpty())
      throw new IllegalArgumentException("segments are required");
    var first = segments.getFirst();
    var merged = new LinkedHashMap<Integer, java.util.Map<String, Object>>();
    segments.stream()
        .sorted(java.util.Comparator.comparing(AdditionalStoreSegment::segmentId))
        .forEach(s -> merged.putAll(s.itemPayloads()));
    return new AdditionalStoreSegment(first.caseId(), "merged", first.connectorType(), merged);
  }
}
