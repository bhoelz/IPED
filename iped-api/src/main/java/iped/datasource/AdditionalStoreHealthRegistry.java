package iped.datasource;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/** Process-local registry used by WebAPI to publish additional-store health snapshots. */
public final class AdditionalStoreHealthRegistry {
    private static final Map<String, Map<String, Integer>> COUNTS = new ConcurrentHashMap<>();
    private AdditionalStoreHealthRegistry() { }
    public static void publish(String caseId, Map<String, Integer> storeCounts) {
        COUNTS.put(caseId, Map.copyOf(storeCounts));
    }
    public static Map<String, Integer> snapshot(String caseId) {
        return COUNTS.getOrDefault(caseId, Map.of());
    }
    public static void clear(String caseId) { COUNTS.remove(caseId); }
}
