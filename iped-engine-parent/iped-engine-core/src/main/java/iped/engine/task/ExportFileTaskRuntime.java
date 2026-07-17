package iped.engine.task;

import iped.data.ICaseData;
import iped.data.IItem;
import java.io.File;
import java.lang.reflect.Method;

public final class ExportFileTaskRuntime {

  private static final String TASK_CLASS = "iped.engine.task.ExportFileTask";

  private ExportFileTaskRuntime() {}

  public static String getExtractDirName() {
    try {
      Class<?> cls = Class.forName("iped.engine.localization.Messages");
      Method m = cls.getMethod("getString", String.class);
      Object out = m.invoke(null, "ExportFileTask.ExportFolder");
      return out instanceof String ? (String) out : "Extracted";
    } catch (Throwable t) {
      return "Extracted";
    }
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

  public static void deleteIgnoredItemData(
      ICaseData caseData, File output, boolean removingEvidence, Object writer) {
    try {
      Class<?> cls = Class.forName(TASK_CLASS);
      Class<?> writerClass = Class.forName("org.apache.lucene.index.IndexWriter");
      Method m =
          cls.getMethod(
              "deleteIgnoredItemData", ICaseData.class, File.class, boolean.class, writerClass);
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
