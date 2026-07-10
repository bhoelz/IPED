package iped.engine.lucene;

import iped.index.spi.IndexedDocument;
import iped.index.spi.IndexingPort;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.NumericDocValuesField;
import org.apache.lucene.document.SortedDocValuesField;
import org.apache.lucene.document.StoredField;
import org.apache.lucene.document.StringField;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.store.ByteBuffersDirectory;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FilterDirectory;
import org.apache.lucene.store.IOContext;
import org.apache.lucene.store.IndexInput;
import org.apache.lucene.util.BytesRef;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for {@link LuceneIndexingAdapter#forEachDocument}, derived from
 * ADR-0002's contract: per-document, cross-field correlation with
 * Lucene-free {@code getString}/{@code getNumeric} accessors that hide
 * whether a field is a stored field or a doc-values field.
 */
class LuceneIndexingAdapterTest {

    private static final String STORED_FIELD = "PATH";
    private static final String SORTED_FIELD = "EVIDENCE_UUID";
    private static final String NUMERIC_FIELD = "ID";
    private static final String ABSENT_FIELD = "DOES_NOT_EXIST";

    private Directory directory;
    private IndexWriter writer;
    private IndexingPort port;

    @BeforeEach
    void setUp() throws IOException {
        directory = new ByteBuffersDirectory();
        writer = new IndexWriter(directory, new IndexWriterConfig(new StandardAnalyzer()));
        port = new LuceneIndexingAdapter(writer);
    }

    @AfterEach
    void tearDown() throws IOException {
        writer.close();
        directory.close();
    }

    private void addDoc(String uuid, String path, long id) throws IOException {
        Document doc = new Document();
        if (uuid != null) {
            doc.add(new StringField(SORTED_FIELD, uuid, Field.Store.NO));
            doc.add(new SortedDocValuesField(SORTED_FIELD, new BytesRef(uuid)));
        }
        if (path != null) {
            doc.add(new StoredField(STORED_FIELD, path));
        }
        doc.add(new NumericDocValuesField(NUMERIC_FIELD, id));
        writer.addDocument(doc);
    }

    @Test
    void forEachDocumentVisitsEveryVisibleDocumentWithCorrelatedFields() throws IOException {
        addDoc("uuid-1", "/root/a.txt", 1L);
        addDoc("uuid-2", "/root/b.txt", 2L);

        List<String> uuids = new ArrayList<>();
        List<String> paths = new ArrayList<>();
        List<Long> ids = new ArrayList<>();
        port.forEachDocument(doc -> {
            uuids.add(doc.getString(SORTED_FIELD));
            paths.add(doc.getString(STORED_FIELD));
            ids.add(doc.getNumeric(NUMERIC_FIELD));
        });

        assertEquals(2, uuids.size());
        assertTrue(uuids.contains("uuid-1") && uuids.contains("uuid-2"));
        assertTrue(paths.contains("/root/a.txt") && paths.contains("/root/b.txt"));
        assertTrue(ids.contains(1L) && ids.contains(2L));
    }

    @Test
    void getStringFallsBackToStoredFieldWhenNoSortedDocValuesPresent() throws IOException {
        addDoc("uuid-1", "/root/a.txt", 1L);

        List<String> paths = new ArrayList<>();
        port.forEachDocument(doc -> paths.add(doc.getString(STORED_FIELD)));

        assertEquals(List.of("/root/a.txt"), paths);
    }

    @Test
    void getStringAndGetNumericReturnNullForAbsentField() throws IOException {
        addDoc("uuid-1", "/root/a.txt", 1L);

        List<String> results = new ArrayList<>();
        List<Long> numericResults = new ArrayList<>();
        port.forEachDocument(doc -> {
            results.add(doc.getString(ABSENT_FIELD));
            numericResults.add(doc.getNumeric(ABSENT_FIELD));
        });

        assertEquals(1, results.size());
        assertNull(results.get(0));
        assertNull(numericResults.get(0));
    }

    @Test
    void forEachDocumentDoesNothingWhenIndexDoesNotYetExist() throws IOException {
        // A brand-new IndexWriter with zero added/committed documents still opens
        // an NRT reader with maxDoc() == 0 in practice; exercise the "no visible
        // documents" branch either way (zero callbacks or empty visits).
        Set<String> visited = new TreeSet<>();
        port.forEachDocument(doc -> visited.add("visited"));

        assertTrue(visited.isEmpty());
    }

    @Test
    void getStringSkipsStoredFieldReadsWhenFieldIsAbsentIndexWide() throws IOException {
        // Regression test for the field-presence fast path: when a field has
        // neither doc-values nor any stored value anywhere in the index (the
        // real-world case of NUM_SUBITEMS/NUM_CARVED_AND_FRAGS being absent
        // early in processing), getString must not fall through to a stored
        // field deserialization on every document -- it should recognize the
        // field is absent (via FieldInfos, checked once per field name) and
        // return null immediately.
        StoredFieldReadCountingDirectory countingDirectory = new StoredFieldReadCountingDirectory(new ByteBuffersDirectory());
        try (IndexWriter countingWriter = new IndexWriter(countingDirectory, new IndexWriterConfig(new StandardAnalyzer()))) {
            IndexingPort countingPort = new LuceneIndexingAdapter(countingWriter);
            for (int i = 0; i < 50; i++) {
                Document doc = new Document();
                doc.add(new StoredField(STORED_FIELD, "/root/" + i + ".txt"));
                doc.add(new NumericDocValuesField(NUMERIC_FIELD, i));
                countingWriter.addDocument(doc);
            }

            int before = countingDirectory.storedFieldOpenCount.get();
            List<String> results = new ArrayList<>();
            countingPort.forEachDocument(doc -> results.add(doc.getString(ABSENT_FIELD)));
            int storedFieldOpensForAbsentFieldScan = countingDirectory.storedFieldOpenCount.get() - before;

            assertEquals(50, results.size());
            assertTrue(results.stream().allMatch(java.util.Objects::isNull));
            // The fast path means opening/reading the stored-fields file happens
            // at most a handful of times (e.g. once to build the reader's
            // metadata), never once per document -- 50 documents with a naive
            // per-doc stored-fields fallback would open/read far more than that.
            assertTrue(storedFieldOpensForAbsentFieldScan < 10,
                    "expected the field-presence fast path to avoid per-document stored-field reads for an absent field, "
                            + "but observed " + storedFieldOpensForAbsentFieldScan + " stored-field file opens for 50 documents");
        }
    }

    /**
     * Counts {@link IndexInput} opens against Lucene's stored-fields data file
     * (the {@code .fdt} extension in every stored-fields format since Lucene
     * 5.0), used as a proxy for "how many times did we actually deserialize a
     * stored document."
     */
    private static final class StoredFieldReadCountingDirectory extends FilterDirectory {
        private final AtomicInteger storedFieldOpenCount = new AtomicInteger();

        StoredFieldReadCountingDirectory(Directory in) {
            super(in);
        }

        @Override
        public IndexInput openInput(String name, IOContext context) throws IOException {
            if (name.endsWith(".fdt")) {
                storedFieldOpenCount.incrementAndGet();
            }
            return super.openInput(name, context);
        }
    }

    @Test
    void secondForEachDocumentCallInSameSessionStillReadsSparseSortedFieldCorrectly() throws IOException {
        // Regression test for a bug where SortedDocValues/NumericDocValues were
        // cached at the SESSION level and reused across multiple forEachDocument
        // calls within the same withSession block. Those doc-values accessors are
        // forward-only iterators: once a first full scan advances through them,
        // reusing the very same instance for a second scan silently returns
        // null/false for every document from that point on -- but only for SPARSE
        // fields (not present on every document), since dense fields can mask the
        // bug via random-access codecs. Here SORTED_FIELD is present on only two
        // of three documents, making it genuinely sparse.
        addDoc("uuid-1", "/root/a.txt", 1L);
        addDoc(null, "/root/b.txt", 2L); // sparse: no SORTED_FIELD on this doc
        addDoc("uuid-3", "/root/c.txt", 3L);

        List<String> firstScanUuids = new ArrayList<>();
        List<String> secondScanUuids = new ArrayList<>();
        port.withSession(session -> {
            session.forEachDocument(doc -> firstScanUuids.add(doc.getString(SORTED_FIELD)));
            session.forEachDocument(doc -> secondScanUuids.add(doc.getString(SORTED_FIELD)));
        });

        List<String> expected = new ArrayList<>();
        expected.add("uuid-1");
        expected.add(null);
        expected.add("uuid-3");

        assertEquals(expected, firstScanUuids, "first scan establishes the baseline");
        assertEquals(expected, secondScanUuids,
                "second forEachDocument call within the same session must re-read the sparse sorted field correctly, "
                        + "not silently return null due to a stale, already-advanced-past SortedDocValues instance "
                        + "reused from the first call");
    }

    @Test
    void perDocumentCorrelationKeepsFieldsOfTheSameDocumentTogether() throws IOException {
        addDoc("uuid-1", "/root/a.txt", 1L);
        addDoc("uuid-2", "/root/b.txt", 2L);

        port.forEachDocument(doc -> {
            String uuid = doc.getString(SORTED_FIELD);
            String path = doc.getString(STORED_FIELD);
            Long id = doc.getNumeric(NUMERIC_FIELD);
            if ("uuid-1".equals(uuid)) {
                assertEquals("/root/a.txt", path);
                assertEquals(1L, id);
            } else if ("uuid-2".equals(uuid)) {
                assertEquals("/root/b.txt", path);
                assertEquals(2L, id);
            }
        });
    }
}
