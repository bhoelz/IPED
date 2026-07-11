package iped.engine.task.carver;

import iped.configuration.Configurable;
import iped.data.IItem;
import iped.engine.config.Configuration;
import iped.engine.config.ConfigurationManager;
import iped.engine.config.EnableTaskProperty;
import iped.engine.config.LocalConfig;
import iped.engine.hashdb.HashDBDataSource;
import iped.engine.hashdb.LedHashDB;
import iped.engine.hashdb.LedItem;
import iped.properties.MediaTypes;
import iped.utils.IOUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.tika.mime.MediaType;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.security.MessageDigest;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

@Slf4j
public class LedCarveTask extends BaseCarveTask {

    private static final String ENABLE_PARAM = "enableLedCarving";


    /**
     * Indica se a tarefa está habilitada ou não.
     */
    private static boolean taskEnabled = false;

    /**
     * Indicador de inicialização, para controle de sincronização entre instâncias
     * da classe.
     */
    private static final AtomicBoolean init = new AtomicBoolean(false);

    /**
     * Case-scoped finalization flag and processing counters. Held in caseData so
     * concurrent/sequential cases don't share or leak each other's counters.
     */
    static final class LedCarveAccumulator {
        static final String KEY = LedCarveAccumulator.class.getName();
        final AtomicBoolean finished = new AtomicBoolean(false);
        final AtomicInteger numCarvedItems = new AtomicInteger();
        final AtomicLong bytesHashed = new AtomicLong();
        final AtomicLong num512total = new AtomicLong();
        final AtomicLong num512hit = new AtomicLong();
    }

    // Used in place of a case-scoped accumulator when caseData is null (i.e.
    // running standalone, no case). Static, consistent with the other carve
    // tasks' accum() fallback (see BaseCarveTask), even though this task doesn't
    // spawn per-item instances itself. Cleared in finish() so successive
    // standalone runs in the same JVM don't share state.
    private static volatile LedCarveAccumulator standaloneAccum;

    private LedCarveAccumulator accum() {
        if (caseData == null) {
            LedCarveAccumulator a = standaloneAccum;
            if (a == null) {
                synchronized (LedCarveTask.class) {
                    a = standaloneAccum;
                    if (a == null) {
                        a = new LedCarveAccumulator();
                        standaloneAccum = a;
                    }
                }
            }
            return a;
        }
        LedCarveAccumulator a = (LedCarveAccumulator) caseData.getCaseObject(LedCarveAccumulator.KEY);
        if (a == null) {
            synchronized (LedCarveTask.class) {
                a = (LedCarveAccumulator) caseData.getCaseObject(LedCarveAccumulator.KEY);
                if (a == null) {
                    a = new LedCarveAccumulator();
                    caseData.putCaseObject(LedCarveAccumulator.KEY, a);
                }
            }
        }
        return a;
    }

    /**
     * Digest utilizado para cálculo do MD5.
     */
    private MessageDigest digest = null;

    /**
     * Base de hashes, com MD5 dos 512 bytes e 64 KBytes iniciais, e respectivos registros na base.
     */
    private static LedHashDB ledHashDB;

    private static HashDBDataSource hashDBDataSource;

    private static final String cachePath = System.getProperty("user.home") + "/.iped/ledcarve.cache";

    @Override
    public boolean isEnabled() {
        return taskEnabled;
    }

    public static void setEnabled(boolean enabled) {
        taskEnabled = enabled;
    }

    @Override
    public List<Configurable<?>> getConfigurables() {
        return Arrays.asList(new EnableTaskProperty(ENABLE_PARAM));
    }

    /**
     * Inicializa tarefa.
     */
    public void init(ConfigurationManager configurationManager) throws Exception {
        synchronized (init) {
            if (!init.get()) {
                boolean enableParam = configurationManager.getEnableTaskProperty(ENABLE_PARAM);
                if (enableParam) {
                    LocalConfig localConfig = (LocalConfig) configurationManager.findObject(LocalConfig.class);
                    File hashDBFile = localConfig.getHashDbFile();
                    if (hashDBFile == null) {
                        log.error("Hashes database path (hashesDB) must be configured in {}", Configuration.LOCAL_CONFIG);
                    } else {
                        if (!hashDBFile.exists() || !hashDBFile.canRead() || !hashDBFile.isFile()) {
                            String msg = (!hashDBFile.exists() ? "Missing": "Invalid") + " hashes database file: " + hashDBFile.getAbsolutePath();
                            if (hasIpedDatasource()) {
                                log.warn(msg);
                            } else {
                                log.error(msg);
                            }
                        } else {
                            hashDBDataSource = new HashDBDataSource(hashDBFile);
                            long t = System.currentTimeMillis();
                            if (readCache(hashDBFile)) {
                                log.info("Load from cache file {}.", cachePath);
                            } else {
                                ledHashDB = hashDBDataSource.readLedHashDB();
                                if (ledHashDB == null || ledHashDB.size() == 0) {
                                    log.error("LED hashes must be loaded into IPED hashes database to enable LedCarveTask.");
                                } else if (writeCache(hashDBFile)) {
                                    log.info("Cache file {} was created.", cachePath);
                                }
                            }
                            if (ledHashDB != null && ledHashDB.size() > 0) {
                                log.info("{} LED Hashes loaded in {} ms.", ledHashDB.size(), System.currentTimeMillis() - t);
                                taskEnabled = true;
                            }
                        }
                    }
                }
                log.info("Task {}.", taskEnabled ? "enabled" : "disabled");
                init.set(true);
            }
        }
        if (taskEnabled) digest = MessageDigest.getInstance("MD5");
    }

    /**
     * Finaliza a tarefa.
     */
    public void finish() throws Exception {
        LedCarveAccumulator a = accum();
        synchronized (a.finished) {
            if (taskEnabled && !a.finished.get()) {
                ledHashDB = null;
                hashDBDataSource.close();
                ledCarved().clear();
                NumberFormat nf = new DecimalFormat("#,##0");
                log.info("Carved files: " + nf.format(a.numCarvedItems.get()));
                log.info("512 blocks (Hits / Total): " + nf.format(a.num512hit.get()) + " / " + nf.format(a.num512total.get()));
                log.info("Bytes hashed: " + nf.format(a.bytesHashed.get()));
                a.finished.set(true);
            }
        }
        if (caseData == null) {
            standaloneAccum = null;
            BaseCarveTask.clearStandaloneAccum();
        }
    }

    protected void process(IItem evidence) throws Exception {
        // Verifica se está desabilitado e se o tipo de arquivo é tratado
        if (!taskEnabled || (caseData != null && caseData.isIpedReport()) || !isAcceptedType((MediaType) evidence.getMediaType()) || !isToProcess(evidence)) return;

        byte[] buf512 = new byte[512];
        byte[] buf64K = new byte[65536 - buf512.length];
        BufferedInputStream is = null;

        int cntCarvedItems = 0;
        long cnt512hit = 0;
        long cnt512total = 0;
        long cntBytesHashed = 0;
        Set<Long> offsets = null;
        try {
            long offset = 0;
            int read512 = 0;
            is = evidence.getBufferedInputStream();
            while ((read512 = is.readNBytes(buf512, 0, buf512.length)) > 0) {
                if (read512 != buf512.length) break;
                cnt512total++;
                boolean empty = true;
                byte first = buf512[0];
                for (int i = 1; i < read512; i++) {
                    if (buf512[i] != first) {
                        empty = false;
                        break;
                    }
                }
                if (!empty) {
                    digest.update(buf512, 0, read512);
                    cntBytesHashed += read512;
                    byte[] hash512 = digest.digest();
                    if (ledHashDB.containsMD5_512(hash512)) {
                        cnt512hit++;
                        is.mark(65536);
                        int read64K = is.readNBytes(buf64K, 0, buf64K.length);
                        is.reset();
                        if (read64K == buf64K.length) {
                            cntBytesHashed += read512 + read64K;
                            digest.update(buf512, 0, read512);
                            digest.update(buf64K, 0, read64K);
                            byte[] hash64K = digest.digest();
                            int hashId = ledHashDB.hashIdFromMD5_64K(hash64K);
                            if (hashId >= 0) {
                                LedItem ledItem = hashDBDataSource.getLedItem(hashId);
                                if (ledItem != null) {
                                    String name = "CarvedLed-" + offset;
                                    String ext = ledItem.getExt();
                                    if (ext != null) name += '.' + ext.toLowerCase();
                                    IItem carvedItem = createCarvedFile(evidence, offset, ledItem.getLength(), name, null);
                                    if (carvedItem != null) {
                                        carvedItem.setExtraAttribute("ledCarvedMD5", ledItem.getMD5());
                                        cntCarvedItems++;
                                        if (offsets == null) {
                                            offsets = new HashSet<Long>();
                                            synchronized (ledCarved()) {
                                                ledCarved().put(evidence, offsets);
                                            }
                                        }
                                        offsets.add(offset);
                                        addOffsetFile(carvedItem, evidence);
                                    }
                                }
                            }
                        }
                    }
                }
                offset += read512;
            }
        } catch (Exception e) {
            log.warn(evidence.toString(), e);
        } finally {
            IOUtil.closeQuietly(is);
        }
        LedCarveAccumulator a = accum();
        a.numCarvedItems.addAndGet(cntCarvedItems);
        a.num512hit.addAndGet(cnt512hit);
        a.num512total.addAndGet(cnt512total);
        a.bytesHashed.addAndGet(cntBytesHashed);
    }

    public static boolean isAcceptedType(MediaType mediaType) {
        return mediaType.getBaseType().equals(UNALLOCATED_MIMETYPE)
                || mediaType.getBaseType().equals(MediaType.OCTET_STREAM)
                || mediaType.getBaseType().equals(MediaTypes.VDI)
                || mediaType.getBaseType().equals(mtPageFile)
                || mediaType.getBaseType().equals(mtVolumeShadow);
    }

    private boolean writeCache(File hashDBFile) {
        File cacheFile = new File(cachePath);
        boolean ret = false;
        DataOutputStream os = null;
        try {
            if (cacheFile.getParentFile() != null && !cacheFile.getParentFile().exists()) {
                cacheFile.getParentFile().mkdirs();
            }
            os = new DataOutputStream(new FileOutputStream(cacheFile));
            os.writeLong(hashDBFile.length());
            os.writeLong(hashDBFile.lastModified());
            os.writeInt(ledHashDB.getMD5_512().length);
            os.write(ledHashDB.getMD5_512());
            os.writeInt(ledHashDB.getMD5_64K().length);
            os.write(ledHashDB.getMD5_64K());
            os.writeInt(ledHashDB.getHashIds().length);
            int[] _arr = ledHashDB.getHashIds(); ByteBuffer _buf = ByteBuffer.allocate(_arr.length * 4).order(ByteOrder.BIG_ENDIAN); _buf.asIntBuffer().put(_arr); os.write(_buf.array());
            ret = true;
        } catch (Exception e) {
            log.warn("Error writing cache file " + cacheFile.getPath(), e);
            return false;
        } finally {
            IOUtil.closeQuietly(os);
            if (!ret) {
                try {
                    cacheFile.delete();
                } catch (Exception e) {}
            }
        }
        return ret;
    }

    private boolean readCache(File hashDBFile) {
        File cacheFile = new File(cachePath);
        if (!cacheFile.exists()) return false;
        DataInputStream is = null;
        boolean ret = false;
        try {
            is = new DataInputStream(new FileInputStream(cacheFile));
            long fileLen = is.readLong();
            if (fileLen == hashDBFile.length()) {
                long fileLastModified = is.readLong();
                if (fileLastModified == hashDBFile.lastModified()) {
                    int len = is.readInt();
                    byte[] md5_512 = IOUtil.readByteArray(is, len);
                    if (md5_512 != null) {
                        len = is.readInt();
                        byte[] md5_64k = IOUtil.readByteArray(is, len);
                        if (md5_64k != null) {
                            len = is.readInt();
                            byte[] _hb = is.readNBytes(len * 4);
                            int[] hashIds = null;
                            if (_hb.length == len * 4) {
                                hashIds = new int[len];
                                ByteBuffer.wrap(_hb).order(ByteOrder.BIG_ENDIAN).asIntBuffer().get(hashIds);
                            }
                            if (hashIds != null) {
                                ledHashDB = new LedHashDB(md5_512, md5_64k, hashIds);
                                ret = true;
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.warn("Error reading cache file " + cacheFile.getPath(), e);
            return false;
        } finally {
            IOUtil.closeQuietly(is);
            if (!ret) {
                try {
                    cacheFile.delete();
                } catch (Exception e) {}
            }
        }
        return ret;
    }
}
