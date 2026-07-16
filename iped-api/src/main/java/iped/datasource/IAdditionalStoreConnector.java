package iped.datasource;

import java.io.Closeable;
import java.io.IOException;
import java.util.Map;

/**
 * Lifecycle contract for pluggable stores that complement the authoritative
 * Lucene case index.
 */
public interface IAdditionalStoreConnector extends Closeable {

    /** Opens or initializes the connector for a case. */
    void open() throws IOException;

    /** Indexes one result and preserves its original evidence item ID. */
    void index(int itemId, String taskName, Map<String, Object> attributes) throws IOException;

    /** Makes pending writes durable according to {@link #consistencyPolicy()}. */
    void commit() throws IOException;

    /** Returns whether this connector can trace every record to an item ID. */
    boolean supportsItemIdTraceability();

    /** Declares the connector's consistency semantics. */
    AdditionalStoreConsistencyPolicy consistencyPolicy();

    @Override
    void close() throws IOException;
}
