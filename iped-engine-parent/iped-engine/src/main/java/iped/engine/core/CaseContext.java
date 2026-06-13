package iped.engine.core;

import iped.data.ICaseData;
import iped.engine.config.ConfigurationView;
import iped.engine.pipeline.EngineHooks;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class CaseContext {

    public enum CaseState {
        QUEUED, RUNNING, PAUSED, COMPLETED, FAILED
    }

    private final UUID id;
    private final Manager manager;
    private final Statistics statistics;
    private final ICaseData caseData;
    private final ConfigurationView configurationView;
    private final EngineHooks hooks;
    private volatile CaseState state;
    private volatile Exception exception;

    /**
     * Set of trackIDs for items already submitted to this case's processing queue.
     * Used by {@code Worker.processNewItem} to skip duplicate submissions, making
     * item processing idempotent under Kafka retry / crash-resume replay.
     */
    private final Set<String> processedTrackIds = ConcurrentHashMap.newKeySet();

    private CaseContext(Builder builder) {
        this.id = builder.id;
        this.manager = builder.manager;
        this.statistics = builder.statistics;
        this.caseData = builder.caseData;
        this.configurationView = builder.configurationView;
        this.hooks = new EngineHooks();
        this.state = CaseState.QUEUED;
        this.exception = null;
    }

    public UUID getId() {
        return id;
    }

    public Manager getManager() {
        return manager;
    }

    public Statistics getStatistics() {
        return statistics;
    }

    public ICaseData getCaseData() {
        return caseData;
    }

    public ConfigurationView getConfigurationView() {
        return configurationView;
    }

    /**
     * Returns the event hooks for this case. External components (e.g.
     * iped-distributed) register their listeners here during case startup
     * to receive job-lifecycle and item-processing events without importing
     * engine or Kafka types.
     *
     * @return the per-case {@link EngineHooks} instance; never {@code null}
     */
    public EngineHooks getHooks() {
        return hooks;
    }

    /**
     * Returns the per-case set of already-submitted trackIDs.
     * Workers use this to implement idempotent item processing: if
     * {@code add(trackId)} returns {@code false} the item was already queued and
     * must be skipped.
     *
     * @return mutable, thread-safe set; never {@code null}
     */
    public Set<String> getProcessedTrackIds() {
        return processedTrackIds;
    }

    public CaseState getState() {
        return state;
    }

    public void setState(CaseState newState) {
        this.state = newState;
    }

    public Exception getException() {
        return exception;
    }

    public void setException(Exception exception) {
        this.exception = exception;
    }

    public static class Builder {
        private UUID id;
        private Manager manager;
        private Statistics statistics;
        private ICaseData caseData;
        private ConfigurationView configurationView;

        public Builder(UUID id) {
            this.id = id != null ? id : UUID.randomUUID();
        }

        public Builder withManager(Manager manager) {
            this.manager = manager;
            return this;
        }

        public Builder withStatistics(Statistics statistics) {
            this.statistics = statistics;
            return this;
        }

        public Builder withCaseData(ICaseData caseData) {
            this.caseData = caseData;
            return this;
        }

        public Builder withConfigurationView(ConfigurationView configurationView) {
            this.configurationView = configurationView;
            return this;
        }

        public CaseContext build() {
            // Manager and Statistics can be null during Phase 1 testing,
            // they will be set in Phase 2/3
            if (caseData == null) {
                throw new IllegalArgumentException("CaseData is required");
            }
            if (configurationView == null) {
                throw new IllegalArgumentException("ConfigurationView is required");
            }
            return new CaseContext(this);
        }
    }

    @Override
    public String toString() {
        return "CaseContext{" +
                "id=" + id +
                ", state=" + state +
                '}';
    }
}
