package iped.engine.task.similarity;

import iped.configuration.Configurable;
import iped.data.IItem;
import iped.engine.config.ConfigurationManager;
import iped.engine.config.EnableTaskProperty;
import iped.engine.task.AbstractTask;
import iped.parsers.util.MetadataUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.tika.mime.MediaType;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Image Similarity task.
 *
 * @author Wladimir Leite
 */
@Slf4j
public class ImageSimilarityTask extends AbstractTask {

    public static final String enableParam = "enableImageSimilarity"; //$NON-NLS-1$

    public static final String IMAGE_FEATURES = "imageFeatures"; //$NON-NLS-1$

    private static boolean taskEnabled = false;
    private static final AtomicBoolean init = new AtomicBoolean(false);
    private static final AtomicBoolean finished = new AtomicBoolean(false);

    private static final AtomicLong totalProcessed = new AtomicLong();
    private static final AtomicLong totalFailed = new AtomicLong();
    private static final AtomicLong totalTime = new AtomicLong();

    private ImageSimilarity imageSimilarity;


    public boolean isEnabled() {
        return taskEnabled;
    }

    public static void setEnabled(boolean enabled) {
        taskEnabled = enabled;
    }

    @Override
    public List<Configurable<?>> getConfigurables() {
        return Arrays.asList(new EnableTaskProperty(enableParam));
    }

    public void init(ConfigurationManager configurationManager) throws Exception {
        synchronized (init) {
            if (!init.get()) {
                taskEnabled = configurationManager.getEnableTaskProperty(enableParam);

                if (!taskEnabled) {
                    log.info("Task disabled."); //$NON-NLS-1$
                    init.set(true);
                    return;
                }

                checkDependency("iped.engine.task.HashTask");
                checkDependency("iped.engine.task.ImageThumbTask");

                log.info("Task enabled."); //$NON-NLS-1$
                init.set(true);
            }
        }
        if (taskEnabled) {
            imageSimilarity = new ImageSimilarity();
        }
    }

    public void finish() throws Exception {
        synchronized (finished) {
            if (taskEnabled && !finished.get()) {
                finished.set(true);
                log.info("Total images processed: " + totalProcessed); //$NON-NLS-1$
                log.info("Total images not processed: " + totalFailed); //$NON-NLS-1$
                long total = totalProcessed.longValue() + totalFailed.longValue();
                if (total != 0) {
                    log.info("Average processing time (milliseconds/image): " + (totalTime.longValue() / total)); //$NON-NLS-1$
                }
            }
        }
    }

    protected void process(IItem evidence) throws Exception {
        if (!taskEnabled || !MetadataUtil.isImageType((MediaType) evidence.getMediaType()) || !evidence.isToAddToCase()
                || evidence.getHash() == null) {
            return;
        }

        Object prev = evidence.getExtraAttribute(IMAGE_FEATURES);
        if (prev != null && prev instanceof byte[]) {
            return;
        }

        try {
            byte[] thumb = evidence.getThumb();
            if (thumb == null) {
                return;
            }
            long t = System.currentTimeMillis();
            BufferedImage img = ImageIO.read(new ByteArrayInputStream(thumb));
            byte[] features = imageSimilarity.extractFeatures(img);
            if (features != null) {
                evidence.setExtraAttribute(IMAGE_FEATURES, features);
                totalProcessed.incrementAndGet();
            } else {
                totalFailed.incrementAndGet();
            }
            t = System.currentTimeMillis() - t;
            totalTime.addAndGet(t);
        } catch (Exception e) {
            log.warn(evidence.toString(), e);
        }
    }
}
