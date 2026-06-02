package iped.engine.webapi.json.v2;

import io.swagger.annotations.ApiModelProperty;

import java.util.Map;

public class EventEnvelopeJSON {
    private String eventType;
    private String version;
    private String timestamp;
    private String caseId;
    private String sessionId;
    private String correlationId;
    private Map<String, Object> payload;

    @ApiModelProperty(required = true)
    public String getEventType() {
        return eventType;
    }

    public void setEventType(String eventType) {
        this.eventType = eventType;
    }

    @ApiModelProperty(required = true)
    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    @ApiModelProperty(required = true)
    public String getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
    }

    @ApiModelProperty(required = true)
    public String getCaseId() {
        return caseId;
    }

    public void setCaseId(String caseId) {
        this.caseId = caseId;
    }

    @ApiModelProperty(required = true)
    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    @ApiModelProperty(required = true)
    public String getCorrelationId() {
        return correlationId;
    }

    public void setCorrelationId(String correlationId) {
        this.correlationId = correlationId;
    }

    @ApiModelProperty(required = true)
    public Map<String, Object> getPayload() {
        return payload;
    }

    public void setPayload(Map<String, Object> payload) {
        this.payload = payload;
    }
}

