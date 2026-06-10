package iped.engine.webapi.json.v2;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class ViewerModelsTest {

    // --- EventEnvelopeJSON ---

    @Test
    void eventEnvelopeJSON_settersAndGetters_roundTrip() {
        EventEnvelopeJSON env = new EventEnvelopeJSON();
        env.setEventType("SEARCH");
        env.setVersion("2.0");
        env.setTimestamp("2023-06-15T10:30:00Z");
        env.setCaseId("case-001");
        env.setSessionId("sess-abc");
        env.setCorrelationId("corr-xyz");
        env.setPayload(Map.of("query", "test"));

        assertEquals("SEARCH", env.getEventType());
        assertEquals("2.0", env.getVersion());
        assertEquals("2023-06-15T10:30:00Z", env.getTimestamp());
        assertEquals("case-001", env.getCaseId());
        assertEquals("sess-abc", env.getSessionId());
        assertEquals("corr-xyz", env.getCorrelationId());
        assertEquals("test", env.getPayload().get("query"));
    }

    @Test
    void eventEnvelopeJSON_defaults_whenNew_thenNull() {
        EventEnvelopeJSON env = new EventEnvelopeJSON();
        assertNull(env.getEventType());
        assertNull(env.getVersion());
        assertNull(env.getCaseId());
        assertNull(env.getPayload());
    }

    // --- HitRangeJSON ---

    @Test
    void hitRangeJSON_settersAndGetters_roundTrip() {
        HitRangeJSON hit = new HitRangeJSON();
        hit.setStart(10);
        hit.setEnd(20);
        hit.setPage(3);

        assertEquals(10, hit.getStart());
        assertEquals(20, hit.getEnd());
        assertEquals(Integer.valueOf(3), hit.getPage());
    }

    @Test
    void hitRangeJSON_defaults_whenNew_thenZeroAndNull() {
        HitRangeJSON hit = new HitRangeJSON();
        assertEquals(0, hit.getStart());
        assertEquals(0, hit.getEnd());
        assertNull(hit.getPage());
    }

    @Test
    void hitRangeJSON_pageCanBeNull() {
        HitRangeJSON hit = new HitRangeJSON();
        hit.setPage(null);
        assertNull(hit.getPage());
    }
}
