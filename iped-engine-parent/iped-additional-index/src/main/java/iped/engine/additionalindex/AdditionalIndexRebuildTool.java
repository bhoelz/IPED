package iped.engine.additionalindex;

import iped.datasource.IAdditionalStoreConnector;

import java.io.IOException;
import java.util.Collection;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;

/**
 * Rebuilds an additional store from projections already present in the case
 * index. It never opens or reprocesses original evidence content.
 */
public final class AdditionalIndexRebuildTool {
    public record RebuildRecord(int itemId, String taskName, Map<String, Object> attributes) {
        public RebuildRecord {
            if (itemId < 0) throw new IllegalArgumentException("itemId must be non-negative");
            Objects.requireNonNull(taskName, "taskName");
            attributes = attributes == null ? Map.of() : Map.copyOf(attributes);
        }
    }

    public record RebuildReport(int requested, int indexed, int failed) { }

    public RebuildReport rebuild(Collection<Integer> itemIds,
                                 IAdditionalStoreConnector connector,
                                 Function<Integer, RebuildRecord> projection) throws IOException {
        Objects.requireNonNull(itemIds, "itemIds");
        Objects.requireNonNull(connector, "connector");
        Objects.requireNonNull(projection, "projection");
        int indexed = 0, failed = 0;
        connector.open();
        try {
            for (Integer itemId : itemIds) {
                try {
                    RebuildRecord record = projection.apply(itemId);
                    if (record == null) { failed++; continue; }
                    connector.index(record.itemId(), record.taskName(), record.attributes());
                    indexed++;
                } catch (RuntimeException ex) {
                    failed++;
                }
            }
            connector.commit();
        } finally {
            connector.close();
        }
        return new RebuildReport(itemIds.size(), indexed, failed);
    }
}
