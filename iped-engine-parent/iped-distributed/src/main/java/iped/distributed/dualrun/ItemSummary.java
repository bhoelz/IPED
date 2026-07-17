package iped.distributed.dualrun;

import iped.distributed.status.ItemStatusEvent;

/**
 * Compact representation of a single evidence item used for dual-run comparison.
 *
 * <p>On the <b>distributed side</b> this is extracted from {@link ItemStatusEvent} (populated at
 * discovery time). On the <b>reference side</b> items are submitted by the operator (or by the
 * monolithic IPED run's output scanner) via the REST API.
 *
 * <p>The {@link #path} field is the canonical matching key between the two sides; {@link #uuid} is
 * the distributed-assigned UUID (null on the reference side when the monolithic path uses a
 * different ID scheme). {@link #mediaType} and {@link #lengthBytes} are optional enrichment used
 * for attribute comparison.
 */
public record ItemSummary(String uuid, String path, String mediaType, Long lengthBytes) {

  /** Extract from a distributed-side status event (DISCOVERED or SUBITEM_DISCOVERED). */
  public static ItemSummary fromEvent(ItemStatusEvent e) {
    return new ItemSummary(e.getItemUuid(), e.getItemPath(), e.getMediaType(), e.getLengthBytes());
  }

  /** Construct a reference-side summary (uuid is not relevant on the monolithic path). */
  public static ItemSummary reference(String path, String mediaType, Long lengthBytes) {
    return new ItemSummary(null, path, mediaType, lengthBytes);
  }
}
