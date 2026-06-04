package iped.engine.webapi.json;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CaseStatusJSONTest {

    @Test
    void settersAndGetters_roundTrip() {
        CaseStatusJSON status = new CaseStatusJSON();
        status.setCaseId("case-002");
        status.setState("COMPLETED");
        status.setItemsProcessed(5000L);
        status.setBytesProcessed(10_000_000L);
        status.setRuntimeSeconds(120L);
        status.setErrorCount(0L);
        status.setEstimatedCompletion("2023-06-15T10:30:00Z");

        assertEquals("case-002", status.getCaseId());
        assertEquals("COMPLETED", status.getState());
        assertEquals(5000L, status.getItemsProcessed());
        assertEquals(10_000_000L, status.getBytesProcessed());
        assertEquals(120L, status.getRuntimeSeconds());
        assertEquals(0L, status.getErrorCount());
        assertEquals("2023-06-15T10:30:00Z", status.getEstimatedCompletion());
    }

    @Test
    void defaults_whenNew_thenNullAndZero() {
        CaseStatusJSON status = new CaseStatusJSON();
        assertNull(status.getCaseId());
        assertNull(status.getState());
        assertEquals(0L, status.getRuntimeSeconds());
        assertNull(status.getEstimatedCompletion());
    }
}
