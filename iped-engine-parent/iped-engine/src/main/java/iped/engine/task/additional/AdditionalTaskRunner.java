package iped.engine.task.additional;

import iped.data.IItem;
import iped.data.IItemId;
import iped.datasource.IAdditionalDataSource;
import iped.engine.config.ConfigurationManager;
import iped.engine.data.IPEDSource;
import iped.engine.task.AbstractTask;
import iped.task.AdditionalProcessingCapable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import lombok.extern.slf4j.Slf4j;

/**
 * Executes one additional-processing task on a user-selected subset of already-indexed items and
 * persists the results to an {@link IAdditionalDataSource}.
 *
 * <h3>Usage</h3>
 *
 * <pre>{@code
 * AdditionalTaskRunner runner = new AdditionalTaskRunner(ipedSource, additionalDataSource);
 * CompletableFuture<AdditionalTaskProgress> future = runner.run(
 *     selectedItems, MyTask.class, progress -> updateUI(progress));
 * }</pre>
 *
 * <h3>Execution model</h3>
 *
 * <ul>
 *   <li>Items are partitioned into batches, one per thread.
 *   <li>Each thread owns one task instance (initialised once via {@link AbstractTask#init}).
 *   <li>Raw (non-enriched) items are fetched via {@link IPEDSource#getRawItemByID} so that
 *       additional processing starts from a clean state.
 *   <li>After all threads finish, {@link IAdditionalDataSource#commit()} is called once to flush
 *       writes.
 * </ul>
 *
 * <h3>Thread safety</h3>
 *
 * Each thread has its own task instance and worker. The only shared mutable state is the {@link
 * IAdditionalDataSource} (which must be thread-safe for concurrent writes — {@link
 * iped.engine.additionalindex.LuceneAdditionalDataSource} satisfies this).
 */
@Slf4j
public class AdditionalTaskRunner {

  private final IPEDSource source;
  private final IAdditionalDataSource destination;
  private final int numThreads;

  /**
   * @param source the case source (used to fetch raw item data)
   * @param destination the additional data source (will receive task results)
   * @param numThreads number of parallel worker threads
   */
  public AdditionalTaskRunner(
      IPEDSource source, IAdditionalDataSource destination, int numThreads) {
    this.source = source;
    this.destination = destination;
    this.numThreads = Math.max(1, numThreads);
  }

  /** Uses {@code availableProcessors / 2} threads (minimum 1). */
  public AdditionalTaskRunner(IPEDSource source, IAdditionalDataSource destination) {
    this(source, destination, Math.max(1, Runtime.getRuntime().availableProcessors() / 2));
  }

  /**
   * Asynchronously processes all selected items with the given task class.
   *
   * @param selectedItems items to process (order is not guaranteed)
   * @param taskClass task class; must be annotated with {@link AdditionalProcessingCapable} and
   *     have a public no-arg constructor
   * @param progressCallback called after each item completes (may be {@code null})
   * @return a future that completes with the final {@link AdditionalTaskProgress} when all items
   *     are done
   */
  public CompletableFuture<AdditionalTaskProgress> run(
      Collection<? extends IItemId> selectedItems,
      Class<? extends AbstractTask> taskClass,
      Consumer<AdditionalTaskProgress> progressCallback) {

    // Copy to a plain List<IItemId> so generics stay simple downstream.
    List<IItemId> itemList = new ArrayList<>();
    for (IItemId id : selectedItems) itemList.add(id);
    return CompletableFuture.supplyAsync(
        () -> {
          try {
            return doRun(itemList, taskClass, progressCallback);
          } catch (Exception e) {
            throw new CompletionException(e);
          }
        });
  }

  // -------------------------------------------------------------------------
  // Internal
  // -------------------------------------------------------------------------

  private AdditionalTaskProgress doRun(
      List<IItemId> items,
      Class<? extends AbstractTask> taskClass,
      Consumer<AdditionalTaskProgress> progressCallback)
      throws Exception {

    int total = items.size();

    AdditionalProcessingCapable annotation =
        taskClass.getAnnotation(AdditionalProcessingCapable.class);
    String displayName =
        (annotation != null) ? annotation.displayName() : taskClass.getSimpleName();

    log.info(
        "Starting additional processing '{}' on {} items using {} threads",
        displayName,
        total,
        numThreads);

    AtomicInteger processed = new AtomicInteger(0);
    AtomicInteger errors = new AtomicInteger(0);
    AtomicBoolean cancelled = new AtomicBoolean(false);

    List<List<IItemId>> batches = partition(items, numThreads);
    ExecutorService pool = Executors.newFixedThreadPool(batches.size());
    List<Future<?>> futures = new ArrayList<>(batches.size());

    for (int t = 0; t < batches.size(); t++) {
      List<IItemId> batch = batches.get(t);
      int workerId = t;
      futures.add(
          pool.submit(
              () -> {
                processBatch(
                    batch,
                    taskClass,
                    displayName,
                    workerId,
                    processed,
                    errors,
                    total,
                    cancelled,
                    progressCallback);
                return null;
              }));
    }

    pool.shutdown();
    try {
      pool.awaitTermination(Long.MAX_VALUE, TimeUnit.MILLISECONDS);
    } catch (InterruptedException e) {
      cancelled.set(true);
      pool.shutdownNow();
      Thread.currentThread().interrupt();
    }

    // Flush all writes to the additional index.
    try {
      destination.commit();
    } catch (Exception e) {
      log.error("Error committing additional index after processing", e);
    }

    AdditionalTaskProgress finalProgress =
        new AdditionalTaskProgress(displayName, processed.get(), total, errors.get());

    log.info(
        "Additional processing '{}' complete: {}/{} succeeded, {} errors",
        displayName,
        processed.get(),
        total,
        errors.get());

    return finalProgress;
  }

  private void processBatch(
      List<IItemId> batch,
      Class<? extends AbstractTask> taskClass,
      String displayName,
      int workerId,
      AtomicInteger processed,
      AtomicInteger errors,
      int total,
      AtomicBoolean cancelled,
      Consumer<AdditionalTaskProgress> progressCallback) {

    AbstractTask task = null;
    try {
      task = taskClass.getDeclaredConstructor().newInstance();
      AdditionalIndexTask sinkTask =
          new AdditionalIndexTask(destination, taskClass.getSimpleName());
      AdditionalTaskWorker worker = new AdditionalTaskWorker(workerId, source.getModuleDir());

      task.setWorker(worker);
      task.setNextTask(sinkTask);
      sinkTask.setWorker(worker);

      task.init(ConfigurationManager.get());

      for (IItemId itemId : batch) {
        if (cancelled.get() || Thread.currentThread().isInterrupted()) break;
        try {
          // Use getRawItemByID to get the unmodified item from the main index.
          IItem item = source.getRawItemByID(itemId.getId());
          if (item != null) {
            task.processAndSendToNextTask(item);
            int n = processed.incrementAndGet();
            if (progressCallback != null) {
              progressCallback.accept(
                  new AdditionalTaskProgress(displayName, n, total, errors.get()));
            }
          } else {
            log.warn("Item {} not found; skipping.", itemId.getId());
            errors.incrementAndGet();
          }
        } catch (Exception e) {
          log.error("Error processing item {} with task '{}'", itemId.getId(), displayName, e);
          errors.incrementAndGet();
        }
      }

      task.finish();

    } catch (Exception e) {
      log.error("Fatal error in additional-processing worker {}", workerId, e);
    }
  }

  private static <T> List<List<T>> partition(List<T> list, int n) {
    List<List<T>> parts = new ArrayList<>();
    int size = list.size();
    int batchSize = Math.max(1, (size + n - 1) / n);
    for (int i = 0; i < size; i += batchSize) {
      parts.add(Collections.unmodifiableList(list.subList(i, Math.min(i + batchSize, size))));
    }
    return parts;
  }
}
