package iped.engine.core;

import iped.configuration.Configurable;
import iped.data.ICaseData;
import iped.data.IItem;
import iped.engine.config.*;
import iped.engine.localization.Messages;
import iped.engine.lucene.ConfiguredFSDirectory;
import iped.engine.task.ExportFileTaskRuntime;
import iped.engine.task.ParsingTaskSupport;
import iped.engine.task.carver.BaseCarveTask;
import iped.engine.task.index.IndexItem;
import iped.engine.util.UIPropertyListenerProvider;
import iped.engine.util.Util;
import iped.exception.IPEDException;
import iped.parsers.standard.StandardParser;
import iped.utils.HashValue;
import lombok.extern.slf4j.Slf4j;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;

import java.io.*;
import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;
import java.util.Date;
import java.util.HashMap;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Classe que armazena estatísticas diversas, como número de itens processados,
 * volume processado, número de timeouts, duplicados ignorados, etc. Contém
 * métodos para enviar as estatísticas para arquivo de log.
 */
@Slf4j
public class Statistics {

    private static final String CARVED_IGNORED_MAP_FILE = "data/carvedIgnoredMap.dat";


    private static final float IO_ERROR_RATE_TO_WARN = 0.05f;

    private HashMap<HashValue, Integer> ignoredMap = new HashMap<>();

    ICaseData caseData;
    File indexDir;

    // Estatísticas
    Date start = new Date();
    int splits = 0;
    int timeouts = 0;
    int processed = 0;
    int activeProcessed = 0;
    long volumeIndexed = 0;
    int lastId = -1;
    int corruptCarveIgnored = 0;
    int ignored = 0;
    int previousIndexedFiles = 0;
    int ioerrors = 0;
    AtomicInteger subitensDiscovered = new AtomicInteger();

    public static Statistics get(ICaseData caseData, File indexDir) {
        return new Statistics(caseData, indexDir);
    }

    public static Statistics get() {
        CaseContext context = CaseContextThreadLocal.get();
        if (context != null) {
            return context.getStatistics();
        }
        return null;
    }

    public int getCarvedIgnoredNum(HashValue trackId) {
        synchronized (ignoredMap) {
            return ignoredMap.getOrDefault(trackId, 0);
        }
    }

    Statistics(ICaseData caseData, File indexDir) {
        this.caseData = caseData;
        this.indexDir = indexDir;
        loadPrevCarvedIgnoredMap();
    }

    public ICaseData getCaseData() {
        return this.caseData;
    }

    private void loadPrevCarvedIgnoredMap() {
        File file = new File(indexDir.getParentFile(), CARVED_IGNORED_MAP_FILE);
        if (file.exists()) {
            try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(file))) {
                ignoredMap = (HashMap<HashValue, Integer>) ois.readObject();

            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }

    public void incCarvedIgnored(IItem item) {
        this.incCorruptCarveIgnored();
        HashValue parentPersistId = new HashValue((String) item.getExtraAttribute(IndexItem.PARENT_TRACK_ID));
        synchronized (ignoredMap) {
            Integer ignored = ignoredMap.getOrDefault(parentPersistId, 0);
            ignoredMap.put(parentPersistId, ++ignored);
        }
    }

    public void resetCarvedIgnored(IItem item) {
        HashValue parentPersistId = new HashValue((String) item.getExtraAttribute(IndexItem.TRACK_ID));
        synchronized (ignoredMap) {
            ignoredMap.remove(parentPersistId);
        }
    }

    public void commit() throws IOException {
        File file = new File(indexDir.getParentFile(), CARVED_IGNORED_MAP_FILE);
        synchronized (ignoredMap) {
            try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(file))) {
                oos.writeObject(ignoredMap);
            }
        }
        Util.fsync(file.toPath());
    }

    synchronized public int getSplits() {
        return splits;
    }

    synchronized public void incSplits() {
        splits++;
    }

    synchronized public int getTimeouts() {
        return timeouts;
    }

    synchronized public void incTimeouts() {
        timeouts++;
    }

    synchronized public void incProcessed() {
        processed++;
    }

    synchronized public int getProcessed() {
        return processed;
    }

    synchronized public void incIoErrors() {
        ioerrors++;
    }

    synchronized public int getIoErrors() {
        return ioerrors;
    }

    synchronized public void incActiveProcessed() {
        activeProcessed++;
    }

    synchronized public int getActiveProcessed() {
        return activeProcessed;
    }

    synchronized public void addVolume(long volume) {
        volumeIndexed += volume;
    }

    synchronized public long getVolume() {
        return volumeIndexed;
    }

    synchronized public int getCorruptCarveIgnored() {
        return corruptCarveIgnored;
    }

    private synchronized void incCorruptCarveIgnored() {
        corruptCarveIgnored++;
    }

    synchronized public int getIgnored() {
        return ignored;
    }

    synchronized public void incIgnored() {
        ignored++;
    }

    synchronized public void updateLastId(int id) {
        if (id > lastId) {
            lastId = id;
        }
    }

    synchronized public int getLastId() {
        return lastId;
    }

    synchronized public void setLastId(int id) {
        lastId = id;
    }

    public void incSubitemsDiscovered() {
        this.subitensDiscovered.incrementAndGet();
    }

    public int getSubitemsDiscovered() {
        return this.subitensDiscovered.get();
    }

    public void logStatistics(Manager manager) throws Exception {

        int processed = getProcessed();
        int extracted = ExportFileTaskRuntime.getItemsExtracted();
        int activeFiles = getActiveProcessed();
        int carvedIgnored = getCorruptCarveIgnored();
        int ignored = getIgnored();

        // Processing times per task
        long totalTime = 0;
        Worker[] workers = manager.getWorkers();
        long[] taskTimes = new long[workers[0].tasks.size()];
        for (Worker worker : workers) {
            for (int i = 0; i < taskTimes.length; i++) {
                long t = worker.tasks.get(i).getTaskTime();
                taskTimes[i] += t;
                totalTime += t;
            }
        }
        LocalConfig localConfig = ConfigurationManager.get().findObject(LocalConfig.class);
        totalTime = totalTime / (1000000 * localConfig.getNumThreads());
        log.info("Processing Times per Task:");
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("%-30s", "TASK"));
        sb.append(String.format(" %7s", "TIME(s)"));
        sb.append(String.format(" %6s", "PCT(%)"));
        log.info(sb.toString());
        sb.setLength(0);
        sb.append(String.format("%-30s", "").replace(' ', '='));
        sb.append(" ").append(String.format("%7s", "").replace(' ', '='));
        sb.append(" ").append(String.format("%6s", "").replace(' ', '='));
        log.info(sb.toString());
        sb.setLength(0);
        for (int i = 0; i < taskTimes.length; i++) {
            long sec = taskTimes[i] / (1000000 * localConfig.getNumThreads());
            sb.append(String.format("%-30s", workers[0].tasks.get(i).getName()));
            sb.append(String.format(" %7d", sec));
            sb.append(String.format(" %6d", Math.round((100f * sec) / totalTime)));
            log.info(sb.toString());
            sb.setLength(0);
        }

        // Processing times per parser
        TreeMap<String, Long> timesPerParser = new TreeMap<String, Long>();
        ParsingTaskSupport.copyTimesPerParser(timesPerParser);
        if (!timesPerParser.isEmpty()) {
            totalTime = 0;
            for (long parserTime : timesPerParser.values()) {
                totalTime += parserTime;
            }
            if (totalTime < 1)
                totalTime = 1;
            sb = new StringBuilder();
            log.info("Processing Times per Parser:");
            sb.append(String.format("%-30s", "PARSER"));
            sb.append(String.format(" %7s", "TIME(s)"));
            sb.append(String.format(" %6s", "PCT(%)"));
            log.info(sb.toString());
            sb.setLength(0);
            sb.append(String.format("%-30s", "").replace(' ', '='));
            sb.append(" ").append(String.format("%7s", "").replace(' ', '='));
            sb.append(" ").append(String.format("%6s", "").replace(' ', '='));
            log.info(sb.toString());
            sb.setLength(0);
            for (String parserName : timesPerParser.keySet()) {
                long time = timesPerParser.get(parserName);
                long sec = time / (1000000 * workers.length);
                sb.append(String.format("%-30s", parserName));
                sb.append(String.format(" %7d", sec));
                sb.append(String.format(" %6d", Math.round(100.0 * time / totalTime)));
                log.info(sb.toString());
                sb.setLength(0);
            }
        }

        int numDocs;
        try (IndexReader reader = DirectoryReader.open(ConfiguredFSDirectory.open(indexDir))) {
            numDocs = reader.numDocs();
        }

        log.info("Partial commits took {} seconds", manager.partialCommitsTime.get());
        log.info("Index internal docs: {}", numDocs); //$NON-NLS-1$
        log.info("Text Splits: {}", getSplits()); //$NON-NLS-1$
        log.info("Timeouts: {}", getTimeouts()); //$NON-NLS-1$
        log.info("Parsing Exceptions: {}", StandardParser.parsingErrors); //$NON-NLS-1$
        log.info("I/O read errors: {}", this.getIoErrors()); //$NON-NLS-1$
        log.info("Subitems Found: {}", getSubitemsDiscovered()); //$NON-NLS-1$
        log.info("Exported Items: {}", extracted); //$NON-NLS-1$
        log.info("Total Carved Items: {}", BaseCarveTask.getItensCarved()); //$NON-NLS-1$
        log.info("Carved Ignored (corrupted): {}", carvedIgnored); //$NON-NLS-1$
        log.info("Ignored Items: {}", ignored); //$NON-NLS-1$

        int indexed = (numDocs - getSplits() - previousIndexedFiles) / 2;
        log.info("Total Indexed: {}", indexed); //$NON-NLS-1$

        log.info("Discovered volume: {} bytes", caseData.getDiscoveredVolume());
        log.info("Processed  volume: {} bytes", getVolume());

        long processedVolume = getVolume() / (1024 * 1024);

        if (activeFiles != processed) {
            log.info("Active Items: {}", activeFiles); //$NON-NLS-1$
        }

        log.info("Total processed: {} items in {} seconds ({} MB)", processed, //$NON-NLS-1$
                ((new Date()).getTime() - start.getTime()) / 1000, processedVolume);

        int discovered = caseData.getDiscoveredEvidences();
        if (processed != discovered) {
            log.error("Alert: Processed " + processed + " items of " + discovered); //$NON-NLS-1$ //$NON-NLS-2$
        }

        // ExportByCategoriesConfig was moved to the iped-tasks-forensics module and is not
        // visible from iped-engine. Use the shared enable property as the engine-level gate
        // for "automatic file export active" (the property is shared by the categories and
        // keywords export configs), combined with the keywords config available here.
        ExportByKeywordsConfig exportByKeywords = ConfigurationManager.get().findObject(ExportByKeywordsConfig.class);
        boolean automaticExportEnabled = ConfigurationManager.get()
                .getEnableTaskProperty(ExportByKeywordsConfig.ENABLE_PARAM);

        if (!(automaticExportEnabled || exportByKeywords.isEnabled())) {
            if (indexed != discovered - carvedIgnored - ignored) {
                log.error("Alert: Indexed " + indexed + " items of " + (discovered - carvedIgnored - ignored)); //$NON-NLS-1$ //$NON-NLS-2$
            }
        } /*
           * else if (indexed != extracted) throw new Exception("Indexados " + indexed +
           * " itens de " + extracted);
           */

        if (this.getIoErrors() > processed * IO_ERROR_RATE_TO_WARN)
            log.error("Warning: IO Errors happened while reading {} items from {}!", getIoErrors(), processed); //$NON-NLS-1$
    }

    public void printSystemInfo() throws Exception {
        LocalConfig localConfig = ConfigurationManager.get().findObject(LocalConfig.class);
        log.info("Operating System: {}", System.getProperty("os.name")); //$NON-NLS-1$ //$NON-NLS-2$
        log.info("Java Version: {}", System.getProperty("java.version")); //$NON-NLS-1$ //$NON-NLS-2$
        String warn = Util.getJavaVersionWarn();
        if (warn != null)
            log.error(warn); // $NON-NLS-1$ //$NON-NLS-2$

        String arch = System.getProperty("os.arch");
        log.info("Architecture: {}", arch); //$NON-NLS-1$
        if (!arch.contains("64")) {
            throw new IPEDException("Java 32 bits not supported anymore. Please update to a 64 bits version.");
        }
        log.info("Current Directory: {}", System.getProperty("user.dir")); //$NON-NLS-1$ //$NON-NLS-2$
        log.info("CPU Cores: {}", Runtime.getRuntime().availableProcessors()); //$NON-NLS-1$
        log.info("numThreads: {}", localConfig.getNumThreads()); //$NON-NLS-1$

        long maxMemory = Runtime.getRuntime().maxMemory() / 1000000;
        log.info("Memory (Heap) Available: {} MB", maxMemory); //$NON-NLS-1$

        for (String path : System.getProperty("java.class.path").split(";")) {
            log.info("ClassPath: {}", path);
        }

        RuntimeMXBean bean = ManagementFactory.getRuntimeMXBean();
        for (String arg : bean.getInputArguments()) {
            arg = arg.replace("\r", "\\r").replace("\n", "\\n");
            log.info("JVM Argument: {}", arg);
        }

        EnableTaskProperty enabledTasks = null;
        for (Configurable<?> config : ConfigurationManager.get().getObjects()) {
            if (config instanceof LocaleConfig || config instanceof PluginConfig) {
                // params already present in LocalConfig
                continue;
            }
            if (config instanceof EnableTaskProperty) {
                enabledTasks = (EnableTaskProperty) config;
                continue;
            }
            String configString = config.getConfiguration().toString().replace("\r\n", " ").replace('\r', ' ').replace('\n', ' ');
            log.info(config.getClass().getSimpleName() + ": " + configString);
        }

        String configString = enabledTasks.getConfiguration().toString();
        log.info(Configuration.CONFIG_FILE + ": " + configString);

        log.info("Java Command: {}", System.getProperty("sun.java.command"));

        int minMemPerThread = 200;
        if (maxMemory / localConfig.getNumThreads() < minMemPerThread) {
            String memoryAlert = Messages.getString("Statistics.LowMemory.Msg").replace("{}", //$NON-NLS-1$
                    Integer.toString(minMemPerThread));
            UIPropertyListenerProvider.getInstance().firePropertyChange("uiWarning", null, //$NON-NLS-1$
                    new EngineMessage(Messages.getString("Statistics.LowMemory.Title"), memoryAlert)); //$NON-NLS-1$
            throw new IPEDException(memoryAlert);
        }

    }

}
