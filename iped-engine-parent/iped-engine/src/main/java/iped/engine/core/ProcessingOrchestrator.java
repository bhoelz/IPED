package iped.engine.core;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;

/**
 * Orchestrates multi-case processing with resource management and scheduling.
 *
 * <p>Coordinates the lifecycle of multiple forensic cases processing concurrently, managing
 * resource allocation, scheduling, and case completion.
 */
@Slf4j
public class ProcessingOrchestrator {

  private static ProcessingOrchestrator instance;

  final ConcurrentHashMap<UUID, CaseContext> activeCases = new ConcurrentHashMap<>();
  private final LinkedBlockingQueue<CaseProcessingRequest> caseQueue = new LinkedBlockingQueue<>();
  private final ResourceManager resourceManager;

  private volatile boolean running = false;
  private Thread schedulerThread;

  private static class CaseProcessingRequest {
    UUID caseId;
    CaseContext context;

    CaseProcessingRequest(UUID caseId, CaseContext context) {
      this.caseId = caseId;
      this.context = context;
    }
  }

  private ProcessingOrchestrator(int maxConcurrentCases, long maxMemoryPerCase) {
    this.resourceManager = new ResourceManager(maxMemoryPerCase, maxConcurrentCases);
  }

  /**
   * Get the singleton ProcessingOrchestrator instance.
   *
   * @return the orchestrator instance
   */
  public static synchronized ProcessingOrchestrator getInstance() {
    if (instance == null) {
      int maxConcurrent = Runtime.getRuntime().availableProcessors();
      long maxMemory = Runtime.getRuntime().maxMemory() / maxConcurrent;
      instance = new ProcessingOrchestrator(maxConcurrent, maxMemory);
    }
    return instance;
  }

  /**
   * Initialize the orchestrator with custom configuration.
   *
   * @param maxConcurrentCases maximum concurrent cases
   * @param maxMemoryPerCase maximum memory per case in bytes
   */
  public static synchronized void initialize(int maxConcurrentCases, long maxMemoryPerCase) {
    instance = new ProcessingOrchestrator(maxConcurrentCases, maxMemoryPerCase);
  }

  /**
   * Enqueue a case for processing.
   *
   * @param caseContext the case context
   * @return the case UUID
   * @throws InterruptedException if interrupted while queueing
   */
  public UUID enqueueCaseForProcessing(CaseContext caseContext) throws InterruptedException {
    UUID caseId = caseContext.getId();
    boolean activated = false;
    synchronized (this) {
      if (activeCases.size() < resourceManager.getMaxConcurrentCases()) {
        try {
          resourceManager.acquireResources(caseId);
          activeCases.put(caseId, caseContext);
          caseContext.setState(CaseContext.CaseState.RUNNING);
          activated = true;
          log.info("Case {} started processing immediately", caseId);
        } catch (IllegalStateException e) {
          // resource limit hit concurrently, fall through to queue
        }
      }
    }
    if (!activated) {
      caseQueue.put(new CaseProcessingRequest(caseId, caseContext));
      log.info("Case {} enqueued for processing", caseId);
    }
    return caseId;
  }

  /** Start processing if not already running. */
  public synchronized void startProcessing() {
    if (!running) {
      running = true;
      schedulerThread = new Thread(this::processQueue);
      schedulerThread.setName("ProcessingOrchestrator-Scheduler");
      schedulerThread.setDaemon(true);
      schedulerThread.start();
      log.info("Processing orchestrator started");
    }
  }

  /**
   * Stop processing and wait for all cases to complete.
   *
   * @throws InterruptedException if interrupted
   */
  public synchronized void stopProcessing() throws InterruptedException {
    running = false;
    if (schedulerThread != null) {
      schedulerThread.join(30000);
    }
    log.info("Processing orchestrator stopped");
  }

  /**
   * Pause case processing.
   *
   * @param caseId the case UUID
   */
  public void pauseCase(UUID caseId) {
    CaseContext context = activeCases.get(caseId);
    if (context != null) {
      context.setState(CaseContext.CaseState.PAUSED);
      log.info("Case {} paused", caseId);
    }
  }

  /**
   * Resume case processing.
   *
   * @param caseId the case UUID
   */
  public void resumeCase(UUID caseId) {
    CaseContext context = activeCases.get(caseId);
    if (context != null) {
      context.setState(CaseContext.CaseState.RUNNING);
      log.info("Case {} resumed", caseId);
    }
  }

  /**
   * Complete processing for a case and release resources.
   *
   * @param caseId the case UUID
   */
  public void completeCaseProcessing(UUID caseId) {
    activeCases.remove(caseId);
    resourceManager.releaseResources(caseId);
    log.info("Case {} processing completed", caseId);
  }

  /**
   * Get a case context by ID.
   *
   * @param caseId the case UUID
   * @return the case context, or null if not found
   */
  public CaseContext getCaseContext(UUID caseId) {
    return activeCases.get(caseId);
  }

  /**
   * Get list of all active case IDs.
   *
   * @return list of active case UUIDs
   */
  public List<UUID> getActiveCaseIds() {
    return new ArrayList<>(activeCases.keySet());
  }

  /**
   * Get the number of active cases.
   *
   * @return number of active cases
   */
  public int getActiveCaseCount() {
    return activeCases.size();
  }

  /**
   * Wait for all cases to complete processing.
   *
   * @param timeoutMs timeout in milliseconds
   * @return true if all cases completed, false if timeout
   * @throws InterruptedException if interrupted
   */
  public boolean waitForCompletion(long timeoutMs) throws InterruptedException {
    long startTime = System.currentTimeMillis();
    while ((System.currentTimeMillis() - startTime) < timeoutMs) {
      if (activeCases.isEmpty() && caseQueue.isEmpty()) {
        return true;
      }
      Thread.sleep(100);
    }
    return false;
  }

  /**
   * Get the resource manager for this orchestrator.
   *
   * @return the resource manager
   */
  public ResourceManager getResourceManager() {
    return resourceManager;
  }

  private void processQueue() {
    while (running) {
      try {
        enforceResourceQuotas();

        CaseProcessingRequest request = caseQueue.poll(1, TimeUnit.SECONDS);
        if (request != null
            && resourceManager.getActiveCaseCount() < resourceManager.getMaxConcurrentCases()) {
          try {
            resourceManager.acquireResources(request.caseId);
            activeCases.put(request.caseId, request.context);
            request.context.setState(CaseContext.CaseState.RUNNING);
            log.info("Case {} started processing", request.caseId);
          } catch (IllegalStateException e) {
            try {
              caseQueue.put(request);
            } catch (InterruptedException ie) {
              log.error("Error re-queueing case", ie);
            }
          }
        } else if (request != null) {
          try {
            caseQueue.put(request);
          } catch (InterruptedException e) {
            log.error("Error queueing case", e);
          }
        }
      } catch (InterruptedException e) {
        log.debug("Orchestrator scheduler interrupted");
      }
    }
  }

  private void enforceResourceQuotas() {
    if (resourceManager.enforceQuotas()) {
      for (UUID caseId : activeCases.keySet()) {
        if (resourceManager.isPaused(caseId)) {
          pauseCase(caseId);
        } else {
          CaseContext context = activeCases.get(caseId);
          if (context != null && context.getState() == CaseContext.CaseState.PAUSED) {
            resumeCase(caseId);
          }
        }
      }
    }
  }
}
