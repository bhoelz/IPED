package iped.engine.task.index;

import iped.configuration.Configurable;
import iped.data.IItem;
import iped.engine.CmdLineArgs;
import iped.engine.config.ConfigurationManager;
import iped.engine.config.IndexTaskConfig;
import iped.engine.core.Worker.STATE;
import iped.engine.data.IPEDSource;
import iped.engine.data.Item;
import iped.engine.index.IndexExtraAttributes;
import iped.engine.index.IndexMetadata;
import iped.engine.io.CloseFilterReader;
import iped.engine.io.FragmentingReader;
import iped.engine.io.ParsingReader;
import iped.engine.task.AbstractTask;
import iped.engine.task.ParsingTaskContextFactory;
import iped.engine.task.ParsingTaskSupport;
import iped.engine.task.SkipCommitedTaskSupport;
import iped.engine.task.carver.BaseCarveTask;
import iped.engine.util.Util;
import iped.exception.IPEDException;
import iped.parsers.standard.StandardParser;
import iped.utils.IOUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.FieldType;
import org.apache.lucene.document.IntPoint;
import org.apache.lucene.index.IndexOptions;
import org.apache.tika.io.TikaInputStream;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.parser.ParseContext;

import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Tarefa de indexação dos itens. Indexa apenas as propriedades, caso a
 * indexação do conteúdo esteja desabilitada. Reaproveita o texto dos itens caso
 * tenha sido extraído por tarefas anteriores.
 *
 * Indexa itens grandes dividindo-os em fragmentos, pois a lib de indexação
 * consome mta memória com documentos grandes.
 *
 */
@Slf4j
public class IndexTask extends AbstractTask {


    public static final String TEXT_SIZE = "textSize"; //$NON-NLS-1$
    public static final String TEXT_SPLITTED = "textSplitted";
    public static final String FRAG_NUM = "fragNum";
    public static final String FRAG_PARENT_ID = "fragParentId";

    private static final AtomicBoolean finished = new AtomicBoolean();
    private static final AtomicBoolean lastIDLoaded = new AtomicBoolean();

    private static FieldType contentField;

    private static final FieldType getContentFieldType() {
        if (contentField == null) {
            FieldType field = new FieldType();
            field.setIndexOptions(IndexOptions.DOCS_AND_FREQS_AND_POSITIONS);
            field.setOmitNorms(true);
            IndexTaskConfig indexConfig = ConfigurationManager.get().findObject(IndexTaskConfig.class);
            field.setStoreTermVectors(indexConfig.isStoreTermVectors());
            field.freeze();
            contentField = field;
        }
        return contentField;
    }

    private StandardParser autoParser;

    private IndexTaskConfig indexConfig;

    public static boolean isTreeNodeOnly(IItem item) {
        return (!item.isToAddToCase() && (item.isDir() || item.isRoot() || item.hasChildren()))
                || item.getExtraAttribute(IndexItem.TREENODE) != null;
    }

    public static void configureTreeNodeAttributes(IItem item) {
        if (item.isSubItem() && item instanceof Item) {
            ((Item) item).dispose(false);
        }
        item.setIdInDataSource(null);
        item.setInputStreamFactory(null);
        item.setExtraAttribute(IndexItem.TREENODE, "true"); //$NON-NLS-1$
        item.getCategorySet().clear();
    }

    public void process(IItem evidence) throws IOException {
        if (evidence.isQueueEnd()) {
            return;
        }

        if (SkipCommitedTaskSupport.isAlreadyCommited(evidence)) {
            evidence.setToIgnore(true);
            return;
        }

        Reader textReader = null;

        if (!evidence.isToAddToCase()) {
            if (isTreeNodeOnly(evidence)) {
                configureTreeNodeAttributes(evidence);
                textReader = new StringReader("");
            } else
                return;
        }

        stats.updateLastId(evidence.getId());

        if (textReader == null) {
            if (indexConfig.isIndexFileContents() && (indexConfig.isIndexUnallocated()
                    || !BaseCarveTask.UNALLOCATED_MIMETYPE.equals(evidence.getMediaType()))) {
                textReader = evidence.getTextReader();
                if (textReader == null) {
                    log.warn("Null Text reader, creating a new one for {}", evidence.getPath()); //$NON-NLS-1$
                    try {
                        TikaInputStream tis = (TikaInputStream) evidence.getTikaStream();
                        Metadata metadata = getMetadata(evidence);
                        final ParseContext context = getTikaContext(evidence);
                        textReader = new ParsingReader(this.autoParser, tis, metadata, context);
                        ((ParsingReader) textReader).startBackgroundParsing();

                    } catch (IOException e) {
                        log.warn("{} Error opening: {} {}", Thread.currentThread().getName(), evidence.getPath(), //$NON-NLS-1$
                                e.toString());
                    }
                }
            }
        }

        if (textReader == null)
            textReader = new StringReader(""); //$NON-NLS-1$

        FragmentingReader fragReader = new FragmentingReader(textReader, indexConfig.getTextSplitSize(),
                indexConfig.getTextOverlapSize());
        try {
            worker.writer.addDocuments(new DocumentsIterable(evidence, fragReader));

        } catch (IOException e) {
            if (IOUtil.isDiskFull(e))
                throw new IPEDException(
                        "Not enough space for the index on " + worker.manager.getIndexTemp().getAbsolutePath()); //$NON-NLS-1$
            else
                throw e;
        } finally {
            fragReader.close();
        }

    }

    private class DocumentsIterable implements Iterable<Document> {

        private IItem item;
        private FragmentingReader fragReader;
        private boolean hasMoreContentFrags, parentIndexed = false;
        private int numFrags = 0;

        private DocumentsIterable(IItem item, FragmentingReader fragReader) {
            this.item = item;
            this.fragReader = fragReader;
        }

        public Iterator<Document> iterator() {
            return new Iterator<Document>() {

                public boolean hasNext() {
                    try {
                        while (worker.state != STATE.RUNNING) {
                            Thread.sleep(1000);
                        }
                        if (Thread.interrupted()) {
                            throw new InterruptedException();
                        }
                        hasMoreContentFrags = (numFrags == 0 || fragReader.nextFragment());
                        return hasMoreContentFrags || !parentIndexed;

                    } catch (InterruptedException | IOException e) {
                        throw new RuntimeException(e);
                    }
                }

                public Document next() {
                    if (hasMoreContentFrags) {
                        if (++numFrags > 1) {
                            stats.incSplits();
                            log.info("{} Splitting text of {}", Thread.currentThread().getName(), item.getPath()); //$NON-NLS-1$
                        }
                        // child (content) document
                        Document doc = new Document();
                        doc.add(new IntPoint(FRAG_NUM, numFrags));
                        doc.add(new IntPoint(FRAG_PARENT_ID, item.getId()));
                        doc.add(new Field(IndexItem.CONTENT, new CloseFilterReader(fragReader), getContentFieldType()));
                        return doc;
                    } else {
                        if (numFrags > 1) {
                            item.setExtraAttribute(TEXT_SPLITTED, Boolean.TRUE.toString());
                        }
                        item.setExtraAttribute(TEXT_SIZE, fragReader.getTotalTextSize());
                        // parent (metadata) document
                        Document doc = IndexItem.Document(item, output);
                        parentIndexed = true;
                        return doc;
                    }
                }

            };
        }

    }

    private Metadata getMetadata(IItem evidence) {
        // new metadata to prevent ConcurrentModificationException while indexing
        Metadata metadata = new Metadata();
        ParsingTaskSupport.fillMetadata(evidence, metadata);
        return metadata;
    }

    private ParseContext getTikaContext(IItem evidence) {
        try {
            return ParsingTaskContextFactory.create(evidence, this.autoParser, ConfigurationManager.get(), worker, false);
        } catch (Exception e) {
            throw new RuntimeException("Error creating parsing context", e);
        }
    }

    @Override
    public List<Configurable<?>> getConfigurables() {
        return Arrays.asList(new IndexTaskConfig());
    }

    @Override
    public void init(ConfigurationManager configurationManager) throws Exception {

        indexConfig = configurationManager.findObject(IndexTaskConfig.class);

        CmdLineArgs args = (CmdLineArgs) caseData.getCaseObject(CmdLineArgs.class.getName());
        if ((args.isAppendIndex() || args.isContinue() || args.isRestart()) && !lastIDLoaded.getAndSet(true)) {
            try (IPEDSource ipedSrc = new IPEDSource(output.getParentFile(), worker.writer)) {
                stats.setLastId(ipedSrc.getLastId());
                Item.setStartID(ipedSrc.getLastId() + 1);
            }
        }

        IndexMetadata.loadMetadataTypes(new File(output, "conf")); //$NON-NLS-1$
        loadExtraAttributes();

        this.autoParser = new StandardParser();

    }

    @Override
    public void finish() throws Exception {

        if (!finished.getAndSet(true)) {
            saveExtraAttributes(output);
            IndexMetadata.saveMetadataTypes(new File(output, "conf")); //$NON-NLS-1$
        }
    }

    public static void saveExtraAttributes(File output) throws IOException {
        IndexExtraAttributes.save(output);
    }

    private void loadExtraAttributes() throws ClassNotFoundException, IOException {

        File extraAttributtesFile = new File(output, "data/" + IndexExtraAttributes.EXTRA_ATTRIBUTES_FILENAME); //$NON-NLS-1$
        if (extraAttributtesFile.exists()) {
            Set<String> extraAttributes = (Set<String>) Util.readObject(extraAttributtesFile.getAbsolutePath());
            Item.getAllExtraAttributes().addAll(extraAttributes);
        }
    }

}

