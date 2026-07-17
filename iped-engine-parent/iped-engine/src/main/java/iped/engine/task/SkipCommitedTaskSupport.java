package iped.engine.task;

import iped.data.IItem;

public final class SkipCommitedTaskSupport {

  public static final String PARENTS_WITH_LOST_SUBITEMS =
      SkipCommitDataKeys.PARENTS_WITH_LOST_SUBITEMS;
  public static final String DATASOURCE_NAMES = SkipCommitDataKeys.DATASOURCE_NAMES;
  public static final String TRACK_ID_ID_MAP = SkipCommitDataKeys.TRACK_ID_ID_MAP;
  public static final String IS_COMMITTED = SkipCommitDataKeys.IS_COMMITTED;

  private SkipCommitedTaskSupport() {}

  private static final String TASK_CLASS = "iped.engine.task.SkipCommitedTask";

  public static boolean isAlreadyCommited(IItem item) {
    try {
      Class<?> cls = Class.forName(TASK_CLASS);
      return (Boolean) cls.getMethod("isAlreadyCommited", IItem.class).invoke(null, item);
    } catch (Throwable t) {
      return false;
    }
  }

  public static void checkAgainLaterProcessedParents(IItem item) {
    try {
      Class<?> cls = Class.forName(TASK_CLASS);
      cls.getMethod("checkAgainLaterProcessedParents", IItem.class).invoke(null, item);
    } catch (Throwable t) {
      // optional runtime integration
    }
  }
}
