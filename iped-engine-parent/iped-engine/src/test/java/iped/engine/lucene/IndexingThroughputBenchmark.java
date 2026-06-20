package iped.engine.lucene;

import iped.configuration.Configurable;
import iped.configuration.IConfigurationDirectory;
import iped.engine.config.ConfigurationManager;
import iped.engine.config.IndexSettings;
import iped.engine.lucene.analysis.AppAnalyzer;
import iped.properties.BasicProps;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.ByteBuffersDirectory;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.TearDown;
import org.openjdk.jmh.annotations.Warmup;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * JMH skeleton benchmark for Lucene indexing throughput (the "items/sec"
 * baseline tracked by the iped-engine roadmap's Phase 4 performance-baseline
 * item). Indexes synthetic documents shaped like a minimal evidence item
 * through the real {@link AppAnalyzer} analyzer chain — exercises the same
 * per-field analysis (tokenization, lowercasing, ASCII folding) every real
 * item goes through, without needing a full case/evidence-set fixture.
 *
 * <p>Run via Maven:
 *   mvn -pl iped-engine-parent/iped-engine test -Dtest=IndexingThroughputBenchmark#main
 */
@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.SECONDS)
@org.openjdk.jmh.annotations.State(Scope.Benchmark)
@Warmup(iterations = 2, time = 1)
@Measurement(iterations = 3, time = 2)
@Fork(1)
public class IndexingThroughputBenchmark {

    private static final String SAMPLE_TEXT = "The quick brown fox jumps over the lazy dog. "
            + "Forensic evidence indexing throughput benchmark sample content repeated for realism. ";

    private Directory directory;
    private IndexWriter writer;
    private AtomicInteger idSeq;

    /** Stub config so {@code AppAnalyzer.get()} finds a registered {@link IndexSettings}. */
    private static final class BenchIndexSettings implements IndexSettings, Configurable<Object> {
        @Override
        public boolean isUseNIOFSDirectory() {
            return false;
        }

        @Override
        public boolean isForceMerge() {
            return false;
        }

        @Override
        public int getCommitIntervalSeconds() {
            return 1800;
        }

        @Override
        public int getMaxTokenLength() {
            return 255;
        }

        @Override
        public boolean isFilterNonLatinChars() {
            return false;
        }

        @Override
        public boolean isConvertCharsToAscii() {
            return false;
        }

        @Override
        public boolean isConvertCharsToLowerCase() {
            return false;
        }

        @Override
        public int[] getExtraCharsToIndex() {
            return null;
        }

        @Override
        public DirectoryStream.Filter<Path> getResourceLookupFilter() {
            return path -> false;
        }

        @Override
        public void processConfig(Path resource) throws IOException {
        }

        @Override
        public Object getConfiguration() {
            return null;
        }

        @Override
        public void setConfiguration(Object config) {
        }
    }

    @Setup(Level.Trial)
    public void setup() throws Exception {
        ConfigurationManager.get().addObject(new BenchIndexSettings());
        Analyzer analyzer = AppAnalyzer.get();
        directory = new ByteBuffersDirectory();
        IndexWriterConfig config = new IndexWriterConfig(analyzer);
        writer = new IndexWriter(directory, config);
        idSeq = new AtomicInteger();
    }

    @TearDown(Level.Trial)
    public void tearDown() throws Exception {
        writer.close();
        directory.close();
    }

    @Benchmark
    public void indexOneDocument() throws IOException {
        Document doc = new Document();
        int id = idSeq.incrementAndGet();
        doc.add(new StringField(BasicProps.ID, Integer.toString(id), Field.Store.YES));
        doc.add(new TextField(BasicProps.NAME, "evidence_item_" + id + ".bin", Field.Store.YES));
        doc.add(new TextField(BasicProps.CONTENT, SAMPLE_TEXT, Field.Store.NO));
        writer.addDocument(doc);
    }

    /** Entry point for running outside of JUnit (no @Test annotation needed). */
    public static void main(String[] args) throws RunnerException {
        Options opt = new OptionsBuilder()
                .include(IndexingThroughputBenchmark.class.getSimpleName())
                .build();
        new Runner(opt).run();
    }
}
