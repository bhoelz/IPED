package iped.engine.additionalindex;

import iped.datasource.AdditionalItemData;
import iped.datasource.IAdditionalDataSource;
import lombok.extern.slf4j.Slf4j;
import org.apache.lucene.analysis.core.KeywordAnalyzer;
import org.apache.lucene.document.*;
import org.apache.lucene.index.*;
import org.apache.lucene.search.*;
import org.apache.lucene.store.FSDirectory;

import java.io.IOException;
import java.nio.file.Path;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * {@link IAdditionalDataSource} implementation backed by a separate Lucene
 * index stored at {@code <case>/iped/.additional-index/}.
 *
 * <h3>Schema</h3>
 * Each Lucene document represents one (itemId, taskName) pair:
 * <ul>
 *   <li>{@code _docKey}    – StringField (indexed + stored): synthetic PK
 *                            {@code "<itemId>_<taskName>"} used for upsert.</li>
 *   <li>{@code _itemId}    – IntPoint (indexed) + StoredField.</li>
 *   <li>{@code _taskName}  – StoredField.</li>
 *   <li>{@code _processedAt} – StoredField (epoch ms as long string).</li>
 *   <li>{@code ea_<key>}   – StoredField per extra attribute,
 *                            value format: {@code "<typeCode>|<encodedValue>"}.</li>
 * </ul>
 *
 * <h3>Type encoding</h3>
 * <table>
 *   <tr><th>Code</th><th>Java type</th></tr>
 *   <tr><td>s</td><td>String</td></tr>
 *   <tr><td>i</td><td>Integer</td></tr>
 *   <tr><td>l</td><td>Long</td></tr>
 *   <tr><td>d</td><td>Double</td></tr>
 *   <tr><td>f</td><td>Float</td></tr>
 *   <tr><td>b</td><td>Boolean</td></tr>
 *   <tr><td>t</td><td>Date (epoch ms)</td></tr>
 * </table>
 *
 * <h3>Thread safety</h3>
 * Concurrent {@link #storeTaskResult} calls are safe (Lucene IndexWriter is
 * thread-safe).  Reader refresh is protected by a read-write lock.
 */
@Slf4j
public class LuceneAdditionalDataSource implements IAdditionalDataSource {


    /** Field name for the synthetic primary key {@code "<itemId>_<taskName>"}. */
    static final String F_DOC_KEY     = "_docKey";       //$NON-NLS-1$
    /** Field name for the integer item ID (IntPoint + StoredField). */
    static final String F_ITEM_ID     = "_itemId";       //$NON-NLS-1$
    /** Field name for the task simple class name. */
    static final String F_TASK_NAME   = "_taskName";     //$NON-NLS-1$
    /** Field name for the processing timestamp (epoch ms). */
    static final String F_PROCESSED_AT = "_processedAt"; //$NON-NLS-1$
    /** Prefix for extra-attribute stored fields. */
    static final String ATTR_PREFIX   = "ea_";           //$NON-NLS-1$

    private final Path indexPath;
    private final FSDirectory directory;
    private final IndexWriter writer;

    private DirectoryReader reader;
    private IndexSearcher searcher;

    /**
     * Guards {@link #reader} / {@link #searcher} access:
     * write-lock for refresh, read-lock for queries.
     */
    private final ReadWriteLock searchLock = new ReentrantReadWriteLock();

    /**
     * Opens (or creates) the additional index at the given path.
     *
     * @param indexPath directory that will hold the Lucene index files
     * @throws IOException if the index cannot be opened or created
     */
    public LuceneAdditionalDataSource(Path indexPath) throws IOException {
        this.indexPath = indexPath;
        this.directory = FSDirectory.open(indexPath);

        IndexWriterConfig cfg = new IndexWriterConfig(new KeywordAnalyzer());
        cfg.setOpenMode(IndexWriterConfig.OpenMode.CREATE_OR_APPEND);
        this.writer = new IndexWriter(directory, cfg);

        // Open a near-real-time reader so we see all committed data immediately.
        this.reader   = DirectoryReader.open(writer);
        this.searcher = new IndexSearcher(reader);
    }

    // -------------------------------------------------------------------------
    // IAdditionalDataSource
    // -------------------------------------------------------------------------

    @Override
    public void storeTaskResult(int itemId, String taskName, Map<String, Object> extraAttrs)
            throws IOException {

        Document doc = new Document();
        String key   = docKey(itemId, taskName);

        // Primary key (for upsert via updateDocument)
        doc.add(new StringField(F_DOC_KEY, key, Field.Store.YES));

        // Item ID – indexed for range queries, stored for reconstruction
        doc.add(new IntPoint(F_ITEM_ID, itemId));
        doc.add(new StoredField(F_ITEM_ID, itemId));

        // Metadata
        doc.add(new StoredField(F_TASK_NAME,    taskName));
        doc.add(new StoredField(F_PROCESSED_AT, String.valueOf(System.currentTimeMillis())));

        // Extra attributes
        if (extraAttrs != null) {
            for (Map.Entry<String, Object> entry : extraAttrs.entrySet()) {
                if (entry.getValue() != null) {
                    doc.add(new StoredField(ATTR_PREFIX + entry.getKey(),
                            encodeValue(entry.getValue())));
                }
            }
        }

        writer.updateDocument(new Term(F_DOC_KEY, key), doc);
    }

    @Override
    public Optional<AdditionalItemData> getTaskResult(int itemId, String taskName) {
        refreshReaderIfNeeded();
        searchLock.readLock().lock();
        try {
            Query q    = new TermQuery(new Term(F_DOC_KEY, docKey(itemId, taskName)));
            TopDocs top = searcher.search(q, 1);
            if (top.totalHits.value() == 0) return Optional.empty();
            Document doc = searcher.storedFields().document(top.scoreDocs[0].doc);
            return Optional.of(docToData(doc));
        } catch (IOException e) {
            log.error("Error reading additional data for item {} task {}", itemId, taskName, e);
            return Optional.empty();
        } finally {
            searchLock.readLock().unlock();
        }
    }

    @Override
    public boolean hasTaskResult(int itemId, String taskName) {
        refreshReaderIfNeeded();
        searchLock.readLock().lock();
        try {
            return searcher.count(new TermQuery(new Term(F_DOC_KEY, docKey(itemId, taskName)))) > 0;
        } catch (IOException e) {
            log.error("Error checking additional data for item {} task {}", itemId, taskName, e);
            return false;
        } finally {
            searchLock.readLock().unlock();
        }
    }

    @Override
    public Set<String> getExecutedTasks(int itemId) {
        refreshReaderIfNeeded();
        searchLock.readLock().lock();
        try {
            Query    q    = IntPoint.newExactQuery(F_ITEM_ID, itemId);
            TopDocs  top  = searcher.search(q, 1_000);
            Set<String> names = new LinkedHashSet<>();
            StoredFields sf   = searcher.storedFields();
            for (ScoreDoc sd : top.scoreDocs) {
                names.add(sf.document(sd.doc).get(F_TASK_NAME));
            }
            return names;
        } catch (IOException e) {
            log.error("Error reading executed tasks for item {}", itemId, e);
            return Collections.emptySet();
        } finally {
            searchLock.readLock().unlock();
        }
    }

    /**
     * Overrides the default interface implementation with a single Lucene query
     * that fetches all task documents for the item at once.
     */
    @Override
    public Map<String, Object> getMergedExtraAttributes(int itemId) {
        refreshReaderIfNeeded();
        searchLock.readLock().lock();
        try {
            Query   q   = IntPoint.newExactQuery(F_ITEM_ID, itemId);
            TopDocs top = searcher.search(q, 1_000);
            Map<String, Object> merged = new LinkedHashMap<>();
            StoredFields sf = searcher.storedFields();
            for (ScoreDoc sd : top.scoreDocs) {
                merged.putAll(extractExtraAttrs(sf.document(sd.doc)));
            }
            return merged;
        } catch (IOException e) {
            log.error("Error merging extra attributes for item {}", itemId, e);
            return Collections.emptyMap();
        } finally {
            searchLock.readLock().unlock();
        }
    }

    @Override
    public void commit() throws IOException {
        writer.commit();
        refreshReaderIfNeeded();
    }

    @Override
    public void close() throws IOException {
        searchLock.writeLock().lock();
        try {
            reader.close();
        } finally {
            searchLock.writeLock().unlock();
        }
        writer.close();
        directory.close();
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private static String docKey(int itemId, String taskName) {
        return itemId + "_" + taskName; //$NON-NLS-1$
    }

    /** Re-opens the reader if the writer has unseen changes. */
    private void refreshReaderIfNeeded() {
        searchLock.writeLock().lock();
        try {
            DirectoryReader fresh = DirectoryReader.openIfChanged(reader);
            if (fresh != null) {
                reader.close();
                reader   = fresh;
                searcher = new IndexSearcher(reader);
            }
        } catch (IOException e) {
            log.warn("Could not refresh additional index reader", e);
        } finally {
            searchLock.writeLock().unlock();
        }
    }

    private AdditionalItemData docToData(Document doc) {
        String taskName     = doc.get(F_TASK_NAME);
        String processedStr = doc.get(F_PROCESSED_AT);
        long   processedMs  = processedStr != null ? Long.parseLong(processedStr) : 0L;
        Map<String, Object> attrs = extractExtraAttrs(doc);
        return new AdditionalItemData(taskName, attrs, Instant.ofEpochMilli(processedMs));
    }

    private static Map<String, Object> extractExtraAttrs(Document doc) {
        Map<String, Object> attrs = new LinkedHashMap<>();
        for (IndexableField field : doc.getFields()) {
            String name = field.name();
            if (name.startsWith(ATTR_PREFIX)) {
                String key   = name.substring(ATTR_PREFIX.length());
                Object value = decodeValue(field.stringValue());
                if (value != null) {
                    attrs.put(key, value);
                }
            }
        }
        return attrs;
    }

    // -------------------------------------------------------------------------
    // Value encoding / decoding
    // -------------------------------------------------------------------------

    /**
     * Encodes an attribute value as {@code "<typeCode>|<stringRepresentation>"}.
     * Unknown types fall back to {@code "s|<toString()>"}.
     */
    static String encodeValue(Object value) {
        if (value instanceof String)  return "s|"  + value;   //$NON-NLS-1$
        if (value instanceof Integer) return "i|"  + value;   //$NON-NLS-1$
        if (value instanceof Long)    return "l|"  + value;   //$NON-NLS-1$
        if (value instanceof Double)  return "d|"  + value;   //$NON-NLS-1$
        if (value instanceof Float)   return "f|"  + value;   //$NON-NLS-1$
        if (value instanceof Boolean) return "b|"  + value;   //$NON-NLS-1$
        if (value instanceof Date)    return "t|"  + ((Date) value).getTime(); //$NON-NLS-1$
        return "s|" + value;  // fallback //$NON-NLS-1$
    }

    /**
     * Decodes a value previously encoded by {@link #encodeValue}.
     * Returns the raw string on parse errors or unknown type codes.
     */
    static Object decodeValue(String raw) {
        if (raw == null) return null;
        int sep = raw.indexOf('|');
        if (sep < 0) return raw;
        String typeCode = raw.substring(0, sep);
        String encoded  = raw.substring(sep + 1);
        try {
            switch (typeCode) {
                case "s":  return encoded;
                case "i":  return Integer.parseInt(encoded);
                case "l":  return Long.parseLong(encoded);
                case "d":  return Double.parseDouble(encoded);
                case "f":  return Float.parseFloat(encoded);
                case "b":  return Boolean.parseBoolean(encoded);
                case "t":  return new Date(Long.parseLong(encoded));
                default:   return encoded;
            }
        } catch (NumberFormatException e) {
            return encoded;
        }
    }
}
