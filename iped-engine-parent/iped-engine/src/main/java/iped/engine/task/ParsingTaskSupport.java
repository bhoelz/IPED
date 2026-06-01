package iped.engine.task;

import java.util.Map;

import org.apache.tika.metadata.Metadata;
import org.apache.tika.metadata.TikaCoreProperties;

import iped.data.IItem;
import iped.parsers.standard.StandardParser;

public final class ParsingTaskSupport {

    public static final String ENCRYPTED = "encrypted"; //$NON-NLS-1$
    public static final String HAS_SUBITEM = "hasSubitem"; //$NON-NLS-1$
    public static final String NUM_SUBITEMS = "numSubItems"; //$NON-NLS-1$

    private ParsingTaskSupport() {
    }

    private static final String TASK_CLASS = "iped.engine.task.ParsingTask";

    public static void fillMetadata(IItem evidence, Metadata metadata) {
        Long len = evidence.getLength();
        if (len != null) {
            metadata.set(Metadata.CONTENT_LENGTH, len.toString());
        }
        metadata.set(TikaCoreProperties.RESOURCE_NAME_KEY, evidence.getName());
        if (evidence.getMediaType() != null) {
            metadata.set(Metadata.CONTENT_TYPE, evidence.getMediaTypeString());
            metadata.set(StandardParser.INDEXER_CONTENT_TYPE, evidence.getMediaTypeString());
        }
        if (evidence.isTimedOut()) {
            metadata.set(StandardParser.INDEXER_TIMEOUT, "true"); //$NON-NLS-1$
        }
    }

    public static void copyTimesPerParser(Map<String, Long> dest) {
        try {
            Class<?> cls = Class.forName(TASK_CLASS);
            cls.getMethod("copyTimesPerParser", Map.class).invoke(null, dest);
        } catch (Throwable t) {
            dest.clear();
        }
    }
}
