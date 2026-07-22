package iped.osint.spi;

import java.net.http.HttpClient;
import java.time.Clock;
import java.util.Map;

public final class OsintContext {

    private final String caseId;
    private final String sourceId;
    private final Integer itemId;
    private final OsintExecutionMode mode;
    private final Map<String, String> secrets;
    private final Map<String, Object> options;
    private final HttpClient httpClient;
    private final Clock clock;

    public OsintContext(String caseId, String sourceId, Integer itemId, OsintExecutionMode mode,
                        Map<String, String> secrets, Map<String, Object> options,
                        HttpClient httpClient, Clock clock) {
        this.caseId = caseId;
        this.sourceId = sourceId;
        this.itemId = itemId;
        this.mode = mode;
        this.secrets = secrets == null ? Map.of() : Map.copyOf(secrets);
        this.options = options == null ? Map.of() : Map.copyOf(options);
        this.httpClient = httpClient;
        this.clock = clock == null ? Clock.systemUTC() : clock;
    }

    public String caseId() {
        return caseId;
    }

    public String sourceId() {
        return sourceId;
    }

    public Integer itemId() {
        return itemId;
    }

    public OsintExecutionMode mode() {
        return mode;
    }

    public Map<String, String> secrets() {
        return secrets;
    }

    public Map<String, Object> options() {
        return options;
    }

    public HttpClient httpClient() {
        return httpClient;
    }

    public Clock clock() {
        return clock;
    }
}
