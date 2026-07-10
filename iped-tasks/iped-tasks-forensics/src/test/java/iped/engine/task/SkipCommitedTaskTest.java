package iped.engine.task;

import iped.engine.CmdLineArgs;
import iped.engine.CmdLineArgsImpl;
import iped.engine.core.Statistics;
import iped.engine.core.Worker;
import iped.engine.data.CaseData;
import iped.engine.task.index.IndexItem;
import iped.properties.BasicProps;
import iped.utils.HashValue;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.NumericDocValuesField;
import org.apache.lucene.document.SortedDocValuesField;
import org.apache.lucene.document.StoredField;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.Field;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.store.ByteBuffersDirectory;
import org.apache.lucene.store.Directory;
import org.apache.lucene.util.BytesRef;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

/**
 * Direct unit tests for {@link SkipCommitedTask}'s migrated business logic
 * (moved off direct Lucene onto {@code IndexingPort}/{@code IndexingSession}
 * in ADR-0002), not just the underlying {@code LuceneIndexingAdapter} port.
 *
 * <p>Covers: (a) {@code collectParentsWithoutAllSubitems} correctly flags
 * parents whose committed subitem count does not match the number of
 * referencing children found in the index; (b) {@code globalToIdMap} is
 * populated correctly by {@code init()}'s scan; (c) the "no prior committed
 * index" scenario -- what replaced the old {@code IndexNotFoundException}
 * catch -- still leaves the task's state correct (empty maps, not null, no
 * exception thrown), consistent with {@code LuceneIndexingAdapter} swallowing
 * that exception and reporting zero documents/values.
 */
class SkipCommitedTaskTest {

    private Directory directory;
    private IndexWriter writer;
    private TestWorker worker;
    private SkipCommitedTask task;

    @AfterEach
    void tearDown() throws Exception {
        task.finish();
        if (writer != null) {
            writer.close();
        }
        if (directory != null) {
            directory.close();
        }
    }

    private void setUp(boolean continueProcessing) throws Exception {
        directory = new ByteBuffersDirectory();
        writer = new IndexWriter(directory, new IndexWriterConfig(new StandardAnalyzer()));

        CaseData caseData = new CaseData();
        CmdLineArgsImpl args = new CmdLineArgsImpl();
        setContinue(args, continueProcessing);
        caseData.putCaseObject(CmdLineArgs.class.getName(), args);
        caseData.putCaseObject(SkipCommitedTaskSupport.DATASOURCE_NAMES, new HashSet<String>());

        worker = new TestWorker(0);
        worker.writer = writer;
        worker.caseData = caseData;
        worker.stats = Statistics.get(caseData, new File(System.getProperty("java.io.tmpdir"), "SkipCommitedTaskTest"));

        task = new SkipCommitedTask();
        task.setWorker(worker);
        resetInitedGuard();
    }

    /**
     * {@code SkipCommitedTask.inited} is a static one-shot guard (mirrors the
     * one-instance-per-Worker-but-init-only-once production design: several
     * Worker threads share the same static state, so only the first Worker's
     * {@code init()} call does the actual index scan). Tests must reset it
     * between cases, since JUnit reuses the same JVM/class across tests.
     */
    private static void resetInitedGuard() throws Exception {
        java.lang.reflect.Field initedField = SkipCommitedTask.class.getDeclaredField("inited");
        initedField.setAccessible(true);
        ((java.util.concurrent.atomic.AtomicBoolean) initedField.get(null)).set(false);
    }

    private static void setContinue(CmdLineArgsImpl args, boolean value) throws Exception {
        java.lang.reflect.Field f = CmdLineArgsImpl.class.getDeclaredField("isContinue");
        f.setAccessible(true);
        f.set(args, value);
    }

    private void addTrackedDoc(String trackId, long id) throws IOException {
        addTrackedDoc(trackId, id, null, null, null, false, false, false);
    }

    private void addTrackedDoc(String trackId, long id, String parentTrackId, Long parentId, String containerTrackId,
            boolean hasChild, boolean subitem, boolean isRoot) throws IOException {
        Document doc = new Document();
        addSorted(doc, BasicProps.TRACK_ID, trackId);
        doc.add(new NumericDocValuesField(IndexItem.ID, id));
        doc.add(new StoredField(IndexItem.ID, id));
        if (parentTrackId != null) {
            addSorted(doc, BasicProps.PARENT_TRACK_ID, parentTrackId);
        }
        if (parentId != null) {
            doc.add(new NumericDocValuesField(BasicProps.PARENTID, parentId));
        }
        if (containerTrackId != null) {
            addSorted(doc, BasicProps.CONTAINER_TRACK_ID, containerTrackId);
        }
        if (hasChild) {
            addSorted(doc, IndexItem.HASCHILD, Boolean.TRUE.toString());
        }
        if (subitem) {
            addSorted(doc, BasicProps.SUBITEM, Boolean.TRUE.toString());
        }
        if (isRoot) {
            addSorted(doc, IndexItem.ISROOT, Boolean.TRUE.toString());
        }
        writer.addDocument(doc);
    }

    private static void addSorted(Document doc, String field, String value) {
        doc.add(new StringField(field, value, Field.Store.YES));
        doc.add(new SortedDocValuesField(field, new BytesRef(value)));
    }

    private void addNumSubitems(String trackId, long id, long numSubitems) throws IOException {
        Document doc = new Document();
        addSorted(doc, BasicProps.TRACK_ID, trackId);
        doc.add(new NumericDocValuesField(IndexItem.ID, id));
        doc.add(new NumericDocValuesField(ParsingTaskSupport.NUM_SUBITEMS, numSubitems));
        writer.addDocument(doc);
    }

    @Test
    void collectParentsWithoutAllSubitemsFlagsOnlyParentsWithMismatchedSubitemCounts() throws Exception {
        setUp(true);

        // parent1 (aa...01) expects 2 subitems and has exactly 2 referencing children: not flagged
        String parent1 = "aa000000000000000000000000000001";
        String parent2 = "aa000000000000000000000000000002";
        addNumSubitems(parent1, 100, 2);
        addTrackedDoc("bb00000000000000000000000000c101", 101, null, null, parent1, false, false, false);
        addTrackedDoc("bb00000000000000000000000000c102", 102, null, null, parent1, false, false, false);

        // parent2 expects 3 subitems but only 1 referencing child exists: flagged
        addNumSubitems(parent2, 200, 3);
        addTrackedDoc("bb00000000000000000000000000c201", 201, null, null, parent2, false, false, false);

        writer.commit();

        task.init(null);

        @SuppressWarnings("unchecked")
        Set<HashValue> lostSubitems = (Set<HashValue>) worker.caseData.getCaseObject(SkipCommitedTask.PARENTS_WITH_LOST_SUBITEMS);

        assertNotNull(lostSubitems);
        assertTrue(lostSubitems.contains(new HashValue(parent2)),
                "parent2 has fewer referencing children than its committed NUM_SUBITEMS and must be flagged");
        assertFalse(lostSubitems.contains(new HashValue(parent1)),
                "parent1's referencing children exactly match its committed NUM_SUBITEMS and must not be flagged");
    }

    @Test
    void globalToIdMapIsPopulatedFromInitScan() throws Exception {
        setUp(true);

        String dirB = "dd000000000000000000000000000b00";
        String ghostParent = "ee00000000000000000000000000ff00";

        // a committed container (HASCHILD=true) must map its trackId -> previous id
        addTrackedDoc(dirB, 400, null, null, null, true, false, false);

        // an item referencing a parent trackId that has no doc of its own in the
        // index (an orphaned/not-yet-committed parent) must still be recorded via
        // its PARENT_TRACK_ID/PARENTID correlation
        addTrackedDoc("aa00000000000000000000000000ca01", 301, ghostParent, 500L, null, false, false, false);

        writer.commit();

        task.init(null);

        @SuppressWarnings("unchecked")
        Map<HashValue, Integer> globalToIdMap = (Map<HashValue, Integer>) worker.caseData
                .getCaseObject(SkipCommitedTask.trackID_ID_MAP);

        assertNotNull(globalToIdMap);
        assertEquals(Integer.valueOf(400), globalToIdMap.get(new HashValue(dirB)));
        assertEquals(Integer.valueOf(500), globalToIdMap.get(new HashValue(ghostParent)));
    }

    @Test
    void initLeavesConsistentEmptyStateWhenNoPriorCommittedIndexExists() throws Exception {
        // No documents are ever added/committed: LuceneIndexingAdapter's NRT reader
        // open either sees maxDoc()==0 or swallows IndexNotFoundException, in both
        // cases yielding zero documents/values -- exactly like the old direct-Lucene
        // code's `catch (IndexNotFoundException e) { commitedtrackIDs = new
        // HashValue[0]; }` branch it replaced.
        setUp(true);

        assertDoesNotThrow(() -> task.init(null));

        @SuppressWarnings("unchecked")
        Set<HashValue> lostSubitems = (Set<HashValue>) worker.caseData.getCaseObject(SkipCommitedTask.PARENTS_WITH_LOST_SUBITEMS);
        @SuppressWarnings("unchecked")
        Map<HashValue, Integer> globalToIdMap = (Map<HashValue, Integer>) worker.caseData
                .getCaseObject(SkipCommitedTask.trackID_ID_MAP);

        assertNotNull(lostSubitems);
        assertTrue(lostSubitems.isEmpty());
        assertNotNull(globalToIdMap);
        assertTrue(globalToIdMap.isEmpty());
    }

    /**
     * Minimal {@link Worker} subclass exposing the protected no-pipeline
     * constructor so the test can wire up {@code writer}/{@code caseData}/
     * {@code stats} directly, without installing the full Task pipeline (which
     * would require a {@code Manager}).
     */
    private static final class TestWorker extends Worker {
        TestWorker(int id) {
            super(id);
        }
    }
}
