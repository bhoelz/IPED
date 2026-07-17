package iped.engine.additionalindex;

import iped.datasource.TimelineEvent;
import org.junit.jupiter.api.Test;
import java.time.Instant;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

class InMemoryTimeSeriesStoreConnectorTest {
    @Test
    void indexesAndQueriesTimelineEventsByWindowAndItem() throws Exception {
        try (var connector = new InMemoryTimeSeriesStoreConnector()) {
            var start = Instant.parse("2026-01-01T00:00:00Z");
            connector.appendEvent(new TimelineEvent(7, start.plusSeconds(10), "login", Map.of("source", "browser")));
            connector.appendEvent(new TimelineEvent(8, start.plusSeconds(20), "message", Map.of()));
            var result = connector.query(start, start.plusSeconds(15), 7, 10);
            assertEquals(1, result.size());
            assertEquals(7, result.getFirst().itemId());
            assertEquals("login", result.getFirst().type());
        }
    }
}
