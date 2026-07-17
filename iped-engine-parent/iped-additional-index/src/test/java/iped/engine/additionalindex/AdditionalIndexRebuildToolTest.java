package iped.engine.additionalindex;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class AdditionalIndexRebuildToolTest {
  @Test
  void rebuildsFromItemProjectionsWithoutReprocessingEvidence() throws Exception {
    var target = new InMemoryTimeSeriesStoreConnector();
    var report =
        new AdditionalIndexRebuildTool()
            .rebuild(
                List.of(1, 2),
                target,
                id ->
                    new AdditionalIndexRebuildTool.RebuildRecord(
                        id, "timeline", Map.of("recovered", true)));
    assertEquals(2, report.indexed());
    assertEquals(0, report.failed());
    assertEquals(2, target.query(null, null, null, 10).size());
  }
}
