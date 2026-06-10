package iped.engine.webapi.json;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class CaseStatsJSONTest {

    @Test
    void settersAndGetters_roundTrip() {
        CaseStatsJSON stats = new CaseStatsJSON();
        stats.setCaseId("case-001");
        stats.setState("RUNNING");
        stats.setItemsProcessed(1000L);
        stats.setBytesProcessed(5_000_000L);
        stats.setMemoryUsage(256_000_000L);
        stats.setErrorCount(3L);

        assertEquals("case-001", stats.getCaseId());
        assertEquals("RUNNING", stats.getState());
        assertEquals(1000L, stats.getItemsProcessed());
        assertEquals(5_000_000L, stats.getBytesProcessed());
        assertEquals(256_000_000L, stats.getMemoryUsage());
        assertEquals(3L, stats.getErrorCount());
    }

    @Test
    void defaults_whenNew_thenNullAndZero() {
        CaseStatsJSON stats = new CaseStatsJSON();
        assertNull(stats.getCaseId());
        assertNull(stats.getState());
        assertEquals(0L, stats.getItemsProcessed());
        assertEquals(0L, stats.getBytesProcessed());
        assertEquals(0L, stats.getMemoryUsage());
        assertEquals(0L, stats.getErrorCount());
    }
}
