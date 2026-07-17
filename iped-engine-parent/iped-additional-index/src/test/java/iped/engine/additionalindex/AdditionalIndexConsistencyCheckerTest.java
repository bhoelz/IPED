package iped.engine.additionalindex;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Map;
import org.junit.jupiter.api.Test;

class AdditionalIndexConsistencyCheckerTest {
  @Test
  void reportsPerStoreDriftAgainstAuthoritativeItemCount() {
    var reports =
        new AdditionalIndexConsistencyChecker().check(10, Map.of("vector", 10, "graph", 8));
    assertTrue(reports.get("vector").consistent());
    assertFalse(reports.get("graph").consistent());
  }
}
