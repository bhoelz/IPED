package dpf.sp.gpinf.indexer.process.task;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.StringReader;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;

import org.apache.commons.compress.archivers.ArchiveStreamFactory;
import org.apache.lucene.document.Document;
import org.apache.tika.extractor.EmbeddedDocumentExtractor;
import org.apache.tika.io.TikaInputStream;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.parser.ParseContext;
import org.apache.tika.parser.Parser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dpf.sp.gpinf.indexer.IndexFiles;
import dpf.sp.gpinf.indexer.io.ParsingReader;
import dpf.sp.gpinf.indexer.parsers.IndexerDefaultParser;
import dpf.sp.gpinf.indexer.parsers.util.IgnoreCorruptedCarved;
import dpf.sp.gpinf.indexer.parsers.util.ItemInfo;
import dpf.sp.gpinf.indexer.process.IndexItem;
import dpf.sp.gpinf.indexer.process.Worker;
import dpf.sp.gpinf.indexer.util.ItemInfoFactory;
import dpf.sp.gpinf.indexer.util.StreamSource;
import dpf.sp.gpinf.indexer.util.Util;
import gpinf.dev.data.EvidenceFile;

/**
 * Tarefa de indexação dos itens. Indexa apenas as propriedades, caso a indexação
 * do conteúdo esteja desabilitada. Reaproveita o texto dos itens caso tenha sido
 * extraído por tarefas anteriores.
 * 
 * Indexa itens grandes dividindo-os em fragmentos, pois a lib de indexação consome
 * mta memória com documentos grandes.
 * 
 */
public class IndexTask extends AbstractTask{
	
	private static Logger LOGGER = LoggerFactory.getLogger(IndexTask.class);
	private static String TEXT_SIZES = IndexTask.class.getSimpleName() + "TEXT_SIZES";
	private static String SPLITED_IDS = IndexTask.class.getSimpleName() + "SPLITED_IDS";

	public static boolean indexFileContents = true;
	public static boolean indexUnallocated = false;
	
	private SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
	
	private List<IdLenPair> textSizes;
	private Set<Integer> splitedIds;
	
	public IndexTask(Worker worker){
		super(worker);
	}
	
	public static class IdLenPair {
		int id, length;

		public IdLenPair(int id, long len) {
			this.id = id;
			this.length = (int) (len / 1000);
		}

	}
	
	public void process(EvidenceFile evidence) throws IOException{
		
		if(!evidence.isToAddToCase() || evidence.isQueueEnd())
			return;
			
		stats.updateLastId(evidence.getId());
		
		String textCache = evidence.getParsedTextCache();
		if (textCache != null) {
			Document doc;
			if (indexFileContents)
				doc = IndexItem.Document(evidence, new StringReader(textCache), dateFormat);
			else
				doc = IndexItem.Document(evidence, null, dateFormat);
			
			worker.writer.addDocument(doc);
			textSizes.add(new IdLenPair(evidence.getId(), textCache.length()));

		} else {
			
			Metadata metadata = getMetadata(evidence);
			ParseContext context = getTikaContext(evidence, evidence.isParsed());
			
			TikaInputStream tis = null;
			try {
				tis = evidence.getTikaStream();
			} catch (IOException e) {
				LOGGER.warn("{} Erro ao abrir: {} {}", Thread.currentThread().getName(), evidence.getPath(), e.toString());
			}
			
			ParsingReader reader = null;
			if ( indexFileContents && tis != null && 
				(indexUnallocated || !CarveTask.UNALLOCATED_MIMETYPE.equals(evidence.getMediaType()))){
					reader = new ParsingReader(worker.autoParser, tis, metadata, context);
					reader.startBackgroundParsing();
			}
			
			Document doc = IndexItem.Document(evidence, reader, dateFormat);
			int fragments = 0;
			try{
				/* Indexa os arquivos dividindo-os em fragmentos, pois a lib de
				 * indexação consome mta memória com documentos grandes
				 */
				do {
					if (++fragments > 1) {
						stats.incSplits();
						if (fragments == 2)
							splitedIds.add(evidence.getId());

						if (IndexFiles.getInstance().verbose)
							LOGGER.info("{} Dividindo texto de {}", Thread.currentThread().getName(), evidence.getPath());
					}

					worker.writer.addDocument(doc);

				} while (!Thread.currentThread().isInterrupted() && reader != null && reader.nextFragment());
				
			}finally{
				if (reader != null)
					reader.reallyClose();
				//comentado pois provoca problema de concorrência com temporaryResources
				//Já é fechado na thread de parsing do parsingReader
				//IOUtil.closeQuietly(tis);
			}
			
			if (reader != null)
				textSizes.add(new IdLenPair(evidence.getId(), reader.getTotalTextSize()));
			else
				textSizes.add(new IdLenPair(evidence.getId(), 0));

		}

		
	}
	
	private Metadata getMetadata(EvidenceFile evidence){
		Metadata metadata = new Metadata();
		Long len = evidence.getLength();
		if(len == null)
			len = 0L;
		metadata.set(Metadata.CONTENT_LENGTH, len.toString());
		metadata.set(Metadata.RESOURCE_NAME_KEY, evidence.getName());
		metadata.set(IndexerDefaultParser.INDEXER_CONTENT_TYPE, evidence.getMediaType().toString());
		if (evidence.isTimedOut())
			metadata.set(IndexerDefaultParser.INDEXER_TIMEOUT, "true");
		return metadata;
	}
	
	private ParseContext getTikaContext(EvidenceFile evidence, final boolean parsed) {
		// DEFINE CONTEXTO: PARSING RECURSIVO, ETC
		ParseContext context = new ParseContext();
		context.set(Parser.class, worker.autoParser);
		ItemInfo itemInfo = ItemInfoFactory.getItemInfo(evidence);
		context.set(ItemInfo.class, itemInfo);
		context.set(StreamSource.class, evidence);
		if(CarveTask.ignoreCorrupted)
			context.set(IgnoreCorruptedCarved.class, new IgnoreCorruptedCarved());
		context.set(EmbeddedDocumentExtractor.class, new ParsingTask(context) {
			@Override
			public boolean shouldParseEmbedded(Metadata arg0) {
				return !parsed;
			}
		});
		
		// Tratamento p/ acentos de subitens de ZIP
		ArchiveStreamFactory factory = new ArchiveStreamFactory();
		factory.setEntryEncoding("Cp850");
		context.set(ArchiveStreamFactory.class, factory);
					
		/*PDFParserConfig config = new PDFParserConfig();
		config.setExtractInlineImages(true);
		context.set(PDFParserConfig.class, config);
		*/
		
		return context;
	}

	@Override
	public void init(Properties properties, File confDir) throws Exception {

		String value = properties.getProperty("indexFileContents");
		if (value != null)
			value = value.trim();
		if (value != null && !value.isEmpty())
			indexFileContents = Boolean.valueOf(value);
		
		value = properties.getProperty("indexUnallocated");
		if (value != null)
			value = value.trim();
		if (value != null && !value.isEmpty())
			indexUnallocated = Boolean.valueOf(value);
		
		textSizes = (List<IdLenPair>) caseData.getCaseObject(TEXT_SIZES);
		if(textSizes == null){
			textSizes = Collections.synchronizedList(new ArrayList<IdLenPair>());
			caseData.putCaseObject(TEXT_SIZES, textSizes);
			
			File prevFile = new File(output, "data/texts.size");
			if(prevFile.exists()){
				FileInputStream fileIn = new FileInputStream(prevFile);
				ObjectInputStream in = new ObjectInputStream(fileIn);

				int[] textSizesArray = (int[]) in.readObject();
				for (int i = 0; i < textSizesArray.length; i++)
					if (textSizesArray[i] != 0)
						textSizes.add(new IdLenPair(i, textSizesArray[i] * 1000L));
			
				in.close();
				fileIn.close();

				stats.setLastId(textSizesArray.length - 1);
				EvidenceFile.setStartID(textSizesArray.length);
			}
		}
		
		splitedIds = (Set<Integer>) caseData.getCaseObject(SPLITED_IDS);
		if(splitedIds == null){
			File prevFile = new File(output, "data/splits.ids");
			if(prevFile.exists()){
				FileInputStream fileIn = new FileInputStream(prevFile);
				ObjectInputStream in = new ObjectInputStream(fileIn);

				splitedIds = (Set<Integer>) in.readObject();

				in.close();
				fileIn.close();
			}else
				splitedIds = Collections.synchronizedSet(new HashSet<Integer>());
			
			caseData.putCaseObject(SPLITED_IDS, splitedIds);
		}
		
	}

	@SuppressWarnings("unchecked")
	@Override
	public void finish() throws Exception {
		
		textSizes = (List<IdLenPair>) caseData.getCaseObject(TEXT_SIZES);
		if(textSizes != null)
			salvarTamanhoTextosExtraidos();
		caseData.putCaseObject(TEXT_SIZES, null);
		
		splitedIds = (Set<Integer>) caseData.getCaseObject(SPLITED_IDS);
		if(splitedIds != null)
			salvarDocsFragmentados();
		caseData.putCaseObject(SPLITED_IDS, null);
		
	}
	
	private void salvarTamanhoTextosExtraidos() throws Exception {

		IndexFiles.getInstance().firePropertyChange("mensagem", "", "Salvando tamanho dos textos extraídos...");
		LOGGER.info("Salvando tamanho dos textos extraídos...");

		int[] textSizesArray = new int[stats.getLastId() + 1];

		for (int i = 0; i < textSizes.size(); i++) {
			IdLenPair pair = textSizes.get(i);
			textSizesArray[pair.id] = pair.length;
		}

		Util.writeObject(textSizesArray, output.getAbsolutePath() + "/data/texts.size");
	}
	
	private void salvarDocsFragmentados() throws Exception {
		IndexFiles.getInstance().firePropertyChange("mensagem", "", "Salvando IDs dos itens fragmentados...");
		LOGGER.info("Salvando IDs dos itens fragmentados...");

		Util.writeObject(splitedIds, output.getAbsolutePath() + "/data/splits.ids");

	}
	
}
