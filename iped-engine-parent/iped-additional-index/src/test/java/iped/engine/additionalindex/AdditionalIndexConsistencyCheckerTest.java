package iped.engine.additionalindex;

import org.junit.jupiter.api.Test;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

class AdditionalIndexConsistencyCheckerTest {
    @Test
    void reportsPerStoreDriftAgainstAuthoritativeItemCount() {
        var reports = new AdditionalIndexConsistencyChecker().check(10, Map.of("vector", 10, "graph", 8));
        assertTrue(reports.get("vector").consistent());
        assertFalse(reports.get("graph").consistent());
    }
}
