package iped.pipeline;

/**
 * Callback interface for item-level processing events within the task pipeline.
 *
 * <p>Implementations receive an {@link ItemProcessingEvent} each time an item finishes processing
 * (success, failure, or skip). This is the API-level hook used by:
 *
 * <ul>
 *   <li>The distributed coordinator — for per-item progress tracking and completion detection
 *       without importing Kafka types.
 *   <li>Monitoring and chain-of-custody audit systems.
 *   <li>Test harnesses that need to verify per-item outcomes.
 * </ul>
 *
 * <p>Implementations must be thread-safe; multiple Worker threads may fire events concurrently.
 */
@FunctionalInterface
public interface IItemProcessingListener {

  /**
   * Called when an item has finished processing through a pipeline task.
   *
   * @param event the item processing event; never {@code null}
   */
  void onItemProcessed(ItemProcessingEvent event);
}
