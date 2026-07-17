package iped.engine.additionalindex;

import iped.datasource.AdditionalStoreSegment;
import org.junit.jupiter.api.Test;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

class SegmentOutputMergerTest {
    @Test
    void mergesSegmentsDeterministicallyByItem() {
        var a = new AdditionalStoreSegment("case", "a", "vector", Map.of(1, Map.of("v", 1)));
        var b = new AdditionalStoreSegment("case", "b", "vector", Map.of(1, Map.of("v", 2), 2, Map.of("v", 3)));
        var merged = new SegmentOutputMerger().merge(java.util.List.of(b, a));
        assertEquals(2, merged.itemPayloads().size());
        assertEquals(2, merged.itemPayloads().get(1).get("v"));
    }
}
