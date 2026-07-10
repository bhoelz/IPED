package iped.arch.fixture;

import org.apache.lucene.index.IndexWriter;

/**
 * Test-only fixture that deliberately violates ADR 0001's seam guard by
 * depending on {@code org.apache.lucene..} directly. Used exclusively by
 * {@code iped.arch.IndexingPortSeamTest#ruleActuallyCatchesALuceneDependencyViolation()}
 * to prove the guard rule fails when it should, instead of trivially
 * passing. Never used by production code.
 */
public class LuceneOffendingTask {

    private IndexWriter writer;

    public void setWriter(IndexWriter writer) {
        this.writer = writer;
    }
}
