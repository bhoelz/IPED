package iped.engine.core;

import iped.data.ICaseData;
import iped.engine.config.ConfigurationView;

import java.util.UUID;

public class CaseContext {

    public enum CaseState {
        QUEUED, RUNNING, PAUSED, COMPLETED, FAILED
    }

    private final UUID id;
    private final Manager manager;
    private final Statistics statistics;
    private final ICaseData caseData;
    private final ConfigurationView configurationView;
    private volatile CaseState state;
    private volatile Exception exception;

    private CaseContext(Builder builder) {
        this.id = builder.id;
        this.manager = builder.manager;
        this.statistics = builder.statistics;
        this.caseData = builder.caseData;
        this.configurationView = builder.configurationView;
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
