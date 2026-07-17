package iped.distributed.dualrun;

import iped.distributed.status.ItemStatusEvent;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;

/**
 * Per-case state for a dual-run comparison.
 *
 * <p>The distributed side is populated incrementally by calling {@link
 * #recordEvent(ItemStatusEvent)} on every status event for this case. Only {@link
 * ItemStatusEvent.Type#DISCOVERED} and {@link ItemStatusEvent.Type#SUBITEM_DISCOVERED} events
 * contribute item records; a {@link ItemStatusEvent.Type#CASE_COMPLETED} event marks the
 * distributed side as finished.
 *
 * <p>The reference side is submitted once via {@link #setReference(List)} after the monolithic run
 * completes.
 *
 * <p>Thread-safe: the distributed map uses {@link ConcurrentHashMap}; {@code referenceByPath} and
 * {@code distributedComplete} are volatile. Callers may call {@link #generateReport()} at any time
 * and will receive a consistent point-in-time snapshot.
 */
@Slf4j
public class DualRunSession {

  private final String caseId;
  private final ConcurrentHashMap<String, ItemSummary> distributedByPath =
      new ConcurrentHashMap<>();
  private volatile Map<String, ItemSummary> referenceByPath = null;
  private volatile boolean distributedComplete = false;

  public DualRunSession(String caseId) {
    this.caseId = caseId;
  }

  /** Feed a status event from the distributed pipeline. */
  public void recordEvent(ItemStatusEvent event) {
    switch (event.getType()) {
      case DISCOVERED, SUBITEM_DISCOVERED -> {
        String path = event.getItemPath();
        if (path != null && !path.isBlank()) {
          distributedByPath.putIfAbsent(path, ItemSummary.fromEvent(event));
        }
      }
      case CASE_COMPLETED -> {
        distributedComplete = true;
        log.info(
            "Dual-run: distributed pipeline finished for case '{}' ({} items discovered)",
            caseId,
            distributedByPath.size());
      }
      default -> {
        /* not tracked */
      }
    }
  }

  /**
   * Submit the reference (monolithic) item set. Should be called once after the reference run
   * completes. Duplicate paths keep the first occurrence.
   */
  public void setReference(List<ItemSummary> items) {
    this.referenceByPath =
        items.stream()
            .filter(s -> s.path() != null && !s.path().isBlank())
            .collect(
                Collectors.toUnmodifiableMap(
                    ItemSummary::path, s -> s, (a, b) -> a)); // keep first on duplicate paths
    log.info(
        "Dual-run: reference submitted for case '{}' ({} items)", caseId, referenceByPath.size());
  }

  /** Generate a point-in-time comparison report. */
  public DualRunReport generateReport() {
    Map<String, ItemSummary> ref = referenceByPath != null ? referenceByPath : Map.of();
    return DualRunComparator.compare(
        caseId, Map.copyOf(distributedByPath), ref, distributedComplete, referenceByPath != null);
  }

  public String getCaseId() {
    return caseId;
  }

  public int getDistributedItemCount() {
    return distributedByPath.size();
  }

  public boolean isDistributedComplete() {
    return distributedComplete;
  }

  public boolean isReferenceSubmitted() {
    return referenceByPath != null;
  }
}
