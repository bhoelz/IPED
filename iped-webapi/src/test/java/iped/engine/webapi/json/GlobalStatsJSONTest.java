package iped.engine.webapi.json;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class GlobalStatsJSONTest {

    @Test
    void settersAndGetters_roundTrip() {
        GlobalStatsJSON stats = new GlobalStatsJSON();
        stats.setActiveCases(3);
        stats.setMaxConcurrentCases(10);
        stats.setTotalMemoryUsed(1_000_000L);
        stats.setMaxMemoryPerCase(500_000L);
        stats.setTotalItemsProcessed(99_000L);

        assertEquals(3, stats.getActiveCases());
        assertEquals(10, stats.getMaxConcurrentCases());
        assertEquals(1_000_000L, stats.getTotalMemoryUsed());
        assertEquals(500_000L, stats.getMaxMemoryPerCase());
        assertEquals(99_000L, stats.getTotalItemsProcessed());
    }

    @Test
    void defaults_whenNew_thenZero() {
        GlobalStatsJSON stats = new GlobalStatsJSON();
        assertEquals(0, stats.getActiveCases());
        assertEquals(0, stats.getMaxConcurrentCases());
        assertEquals(0L, stats.getTotalMemoryUsed());
        assertEquals(0L, stats.getMaxMemoryPerCase());
        assertEquals(0L, stats.getTotalItemsProcessed());
    }
}
