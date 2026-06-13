package iped.scripting;

import iped.datasource.IDataSource;
import iped.pipeline.IJobLifecycleListener;
import iped.pipeline.IItemProcessingListener;
import iped.pipeline.JobLifecycleEvent;

import java.io.File;
import java.util.UUID;
import java.util.concurrent.Future;

/**
 * Scripting-SDK service for submitting and monitoring processing jobs.
 *
 * <p>Scripts use this interface to start new IPED processing jobs and observe
 * their progress without importing engine-internal types.
 *
 * @since 4.2
 */
public interface IJobOrchestrator {

    /**
     * Submits a new processing job for the given data source against the
     * specified output directory and returns a handle to the running job.
     *
     * <p>The call is non-blocking. Use the returned {@link Future} or register
     * listeners via {@link #addJobListener} to track completion.
     *
     * @param dataSource    the evidence to process; must not be {@code null}
     * @param outputDir     directory where the case will be written; must not be {@code null}
     * @param profileDir    profile/config directory to use; may be {@code null} to use defaults
     * @return a {@link Future} whose value is the case UUID assigned to the job;
     *         never {@code null}
     */
    Future<UUID> submit(IDataSource dataSource, File outputDir, File profileDir);

    /**
     * Cancels the job identified by {@code caseId} if it is still running.
     * A no-op if the job has already finished or the ID is unknown.
     *
     * @param caseId the case UUID returned by {@link #submit}; must not be {@code null}
     */
    void cancel(UUID caseId);

    /**
     * Blocks until the job identified by {@code caseId} has reached a terminal
     * state ({@code COMPLETED} or {@code FAILED}).
     *
     * @param caseId the case UUID to wait for; must not be {@code null}
     * @throws InterruptedException if the current thread is interrupted while waiting
     */
    void awaitCompletion(UUID caseId) throws InterruptedException;

    /**
     * Returns the last known lifecycle event for the job, or {@code null} if
     * no event has been fired yet or the ID is unknown.
     *
     * @param caseId the case UUID; must not be {@code null}
     * @return last lifecycle event, or {@code null}
     */
    JobLifecycleEvent getLastEvent(UUID caseId);

    /**
     * Registers a listener that will receive job lifecycle events for all jobs
     * managed by this orchestrator. Idempotent if called twice with the same instance.
     *
     * @param listener listener to add; must not be {@code null}
     */
    void addJobListener(IJobLifecycleListener listener);

    /**
     * Removes a previously registered job lifecycle listener. A no-op if the
     * listener was not registered.
     *
     * @param listener listener to remove; must not be {@code null}
     */
    void removeJobListener(IJobLifecycleListener listener);

    /**
     * Registers a listener that will receive item processing events for all jobs
     * managed by this orchestrator. Idempotent if called twice with the same instance.
     *
     * @param listener listener to add; must not be {@code null}
     */
    void addItemListener(IItemProcessingListener listener);

    /**
     * Removes a previously registered item processing listener. A no-op if the
     * listener was not registered.
     *
     * @param listener listener to remove; must not be {@code null}
     */
    void removeItemListener(IItemProcessingListener listener);
}
