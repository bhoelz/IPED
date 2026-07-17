package iped.engine.additionalindex;

import java.util.LinkedHashMap;
import java.util.Map;

/** Compares each additional-store count with the authoritative case item count. */
public final class AdditionalIndexConsistencyChecker {
  public record StoreReport(String store, int expectedItems, int actualItems, boolean consistent) {}

  public Map<String, StoreReport> check(int expectedItems, Map<String, Integer> storeCounts) {
    Map<String, StoreReport> result = new LinkedHashMap<>();
    storeCounts.forEach(
        (name, count) ->
            result.put(
                name,
                new StoreReport(
                    name, expectedItems, count, count != null && count == expectedItems)));
    return result;
  }
}
