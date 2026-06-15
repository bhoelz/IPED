package iped.runner.execution;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;

/**
 * Priority run queue — limits concurrent IPED launches to
 * {@code runner.max-concurrent} (default 2) and drains queued
 * {@link QueuedRun}s in priority order.
 *
 * <p>The queue is flushed on service shutdown; queued (not yet started) runs
 * are abandoned — they are not persisted across restarts.
 */
@Service
@Slf4j
public class RunQueueService {

    @Value("${runner.max-concurrent:2}")
    private int maxConcurrent;

    @Value("${runner.profiles-dir:profiles}")
    private String profilesDir;

    private final ExecutionService executionService;

    public RunQueueService(ExecutionService executionService) {
        this.executionService = executionService;
    }

    private final PriorityBlockingQueue<QueuedRun> queue =
            new PriorityBlockingQueue<>(16, Comparator
                    .comparingInt((QueuedRun r) -> -r.priority().weight())
                    .thenComparing(QueuedRun::enqueuedAt));

    private final Semaphore slots = new Semaphore(0);  // initialised in @PostConstruct
    private final ExecutorService drainer = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "runner-queue-drainer");
        t.setDaemon(true);
        return t;
    });

    @PostConstruct
    void init() {
        slots.release(maxConcurrent);
        // Register this service so ExecutionService can notify us when a slot opens.
        executionService.setQueueService(this);
        drainer.submit(this::drainLoop);
        log.info("RunQueueService started, max-concurrent={}, profiles-dir={}", maxConcurrent, profilesDir);
    }

    /**
     * Enqueues a run.  If a slot is immediately available, the run may start
     * before this method returns.
     *
     * @return the {@link QueuedRun} token (its {@code id} tracks the run)
     */
    public QueuedRun enqueue(RunRequest req, RunPriority priority) {
        var run = new QueuedRun(UUID.randomUUID().toString(), req, priority,
                java.time.Instant.now());
        queue.add(run);
        log.info("Enqueued run {} (priority={}), queue depth={}", run.id(), priority, queue.size());
        return run;
    }

    /** Returns a snapshot of all waiting (not yet started) runs, in dispatch order. */
    public List<QueuedRun> pending() {
        var list = new ArrayList<>(queue);
        list.sort(Comparator
                .comparingInt((QueuedRun r) -> -r.priority().weight())
                .thenComparing(QueuedRun::enqueuedAt));
        return Collections.unmodifiableList(list);
    }

    /**
     * Returns the profile names available under {@code runner.profiles-dir}.
     * Profile files are {@code *.toml} files.  Returns an empty list if the
     * directory does not exist.
     */
    public List<String> availableProfiles() {
        Path dir = Path.of(profilesDir);
        if (!Files.isDirectory(dir)) return List.of();
        try (var stream = Files.list(dir)) {
            return stream
                    .filter(p -> p.toString().endsWith(".toml"))
                    .map(p -> p.getFileName().toString().replaceFirst("\\.toml$", ""))
                    .sorted()
                    .toList();
        } catch (IOException e) {
            log.warn("Could not list profiles in {}: {}", profilesDir, e.getMessage());
            return List.of();
        }
    }

    // -------------------------------------------------------------------------
    // Internal
    // -------------------------------------------------------------------------

    private void drainLoop() {
        while (!Thread.currentThread().isInterrupted()) {
            try {
                QueuedRun run = queue.take(); // blocks until one is available
                slots.acquire();             // blocks until a concurrent slot opens
                log.info("Dispatching queued run {} (priority={})", run.id(), run.priority());
                try {
                    executionService.start(run.request());
                } catch (IOException e) {
                    log.error("Failed to start queued run {}: {}", run.id(), e.getMessage());
                    slots.release(); // free the slot so the next run can proceed
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.info("Queue drainer interrupted — shutting down");
            }
        }
    }

    /** Called by {@link ExecutionService} when a run reaches a terminal state. */
    void onRunTerminated() {
        slots.release();
        log.debug("Slot released; pending queue depth={}", queue.size());
    }
}
