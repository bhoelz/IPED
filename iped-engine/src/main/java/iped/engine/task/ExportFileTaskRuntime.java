package iped.engine.task;

import java.io.File;
import java.lang.reflect.Method;

import org.apache.lucene.index.IndexWriter;

import iped.data.ICaseData;
import iped.data.IItem;
import iped.engine.localization.Messages;

public final class ExportFileTaskRuntime {

    private static final String TASK_CLASS = "iped.engine.task.ExportFileTask";

    private ExportFileTaskRuntime() {
    }

    public static String getExtractDirName() {
        return Messages.getString("ExportFileTask.ExportFolder");
    }

    public static int getItemsExtracted() {
        try {
            Class<?> cls = Class.forName(TASK_CLASS);
            Method m = cls.getMethod("getItensExtracted");
            Object out = m.invoke(null);
            return out instanceof Number ? ((Number) out).intValue() : 0;
        } catch (Throwable t) {
            return 0;
        }
    }

    public static void commitStorage(File output) {
        try {
            Class<?> cls = Class.forName(TASK_CLASS);
            Method m = cls.getMethod("commitStorage", File.class);
            m.invoke(null, output);
        } catch (Throwable t) {
            // optional runtime integration
        }
    }

    public static void deleteIgnoredItemData(ICaseData caseData, File output) {
        try {
            Class<?> cls = Class.forName(TASK_CLASS);
            Method m = cls.getMethod("deleteIgnoredItemData", ICaseData.class, File.class);
            m.invoke(null, caseData, output);
        } catch (Throwable t) {
            // optional runtime integration
        }
    }

    public static void deleteIgnoredItemData(ICaseData caseData, File output, boolean removingEvidence,
            IndexWriter writer) {
        try {
            Class<?> cls = Class.forName(TASK_CLASS);
            Method m = cls.getMethod("deleteIgnoredItemData", ICaseData.class, File.class, boolean.class,
                    IndexWriter.class);
            m.invoke(null, caseData, output, removingEvidence, writer);
        } catch (Throwable t) {
            // optional runtime integration
        }
    }

    public static void insertIntoStorage(IItem evidence, byte[] buf, int len) {
        try {
            Class<?> cls = Class.forName(TASK_CLASS);
            Method getLast = cls.getMethod("getLastInstance");
            Object instance = getLast.invoke(null);
            if (instance == null) {
                return;
            }
            Method m = cls.getMethod("insertIntoStorage", IItem.class, byte[].class, int.class);
            m.invoke(instance, evidence, buf, len);
        } catch (Throwable t) {
            // optional runtime integration
        }
    }
}
