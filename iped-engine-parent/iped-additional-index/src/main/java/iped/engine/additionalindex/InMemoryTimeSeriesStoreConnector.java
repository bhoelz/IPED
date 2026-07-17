package iped.engine.additionalindex;

import iped.datasource.AdditionalStoreConsistencyPolicy;
import iped.datasource.ITimeSeriesStoreConnector;
import iped.datasource.TimelineEvent;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

/** Reference time-series connector for local use and contract tests. */
public final class InMemoryTimeSeriesStoreConnector implements ITimeSeriesStoreConnector {
    private final List<TimelineEvent> events = new CopyOnWriteArrayList<>();
    @Override public void open() { }
    @Override public void index(int itemId, String taskName, Map<String, Object> attributes) { appendEvent(new TimelineEvent(itemId, Instant.now(), taskName, attributes)); }
    @Override public void appendEvent(TimelineEvent event) { events.add(Objects.requireNonNull(event)); }
    @Override public List<TimelineEvent> query(Instant from, Instant to, Integer itemId, int limit) {
        Instant start = from == null ? Instant.MIN : from, end = to == null ? Instant.MAX : to;
        return events.stream().filter(e -> !e.timestamp().isBefore(start) && !e.timestamp().isAfter(end))
                .filter(e -> itemId == null || e.itemId() == itemId)
                .sorted(Comparator.comparing(TimelineEvent::timestamp)).limit(Math.max(0, limit)).toList();
    }
    @Override public void commit() { }
    @Override public boolean supportsItemIdTraceability() { return true; }
    @Override public AdditionalStoreConsistencyPolicy consistencyPolicy() { return AdditionalStoreConsistencyPolicy.COMMIT_ATOMIC; }
    @Override public void close() { }
}
