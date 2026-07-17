package iped.engine.task;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.lang.reflect.Constructor;
import org.junit.jupiter.api.Test;

class TaskConstantsTest {

  // ── HashDBConstants ──────────────────────────────────────────────────────

  @Test
  void hashDBConstants_knownValue_equalsExpectedString() {
    assertEquals("known", HashDBConstants.KNOWN_VALUE);
  }

  @Test
  void hashDBConstants_privateConstructor_isInvocableViaReflection() throws Exception {
    Constructor<HashDBConstants> c = HashDBConstants.class.getDeclaredConstructor();
    c.setAccessible(true);
    assertNotNull(c.newInstance());
  }

  // ── SkipCommitDataKeys ───────────────────────────────────────────────────

  @Test
  void skipCommitDataKeys_constants_matchExpectedValues() {
    assertEquals("PARENTS_WITH_LOST_SUBITEMS", SkipCommitDataKeys.PARENTS_WITH_LOST_SUBITEMS);
    assertEquals("CMD_LINE_DATASOURCE_NAMES", SkipCommitDataKeys.DATASOURCE_NAMES);
    assertEquals("trackID_ID_MAP", SkipCommitDataKeys.TRACK_ID_ID_MAP);
    assertEquals("IS_COMMITTED", SkipCommitDataKeys.IS_COMMITTED);
  }

  @Test
  void skipCommitDataKeys_privateConstructor_isInvocableViaReflection() throws Exception {
    Constructor<SkipCommitDataKeys> c = SkipCommitDataKeys.class.getDeclaredConstructor();
    c.setAccessible(true);
    assertNotNull(c.newInstance());
  }

  // ── TaskRuntime (fallback paths when target class is absent) ─────────────

  @Test
  void taskRuntime_invokeStaticVoid_unknownClass_swallowsThrowable() {
    // Must not throw even when the class does not exist on the classpath
    TaskRuntime.invokeStaticVoid("iped.engine.task.NonExistentClass", "someMethod");
  }

  @Test
  void taskRuntime_invokeStaticVoidWithParam_unknownClass_swallowsThrowable() {
    TaskRuntime.invokeStaticVoid(
        "iped.engine.task.NonExistentClass", "someMethod", String.class, "param");
  }

  @Test
  void taskRuntime_invokeVideoScore_unknownClass_returnsFallback() {
    double result =
        TaskRuntime.invokeVideoScore("iped.engine.task.NonExistentClass", java.util.Map.of(), 3.14);
    assertEquals(3.14, result, 1e-9);
  }

  @Test
  void taskRuntime_invokeVideoScoreList_unknownClass_returnsFallback() {
    double result =
        TaskRuntime.invokeVideoScoreList(
            "iped.engine.task.NonExistentClass", java.util.List.of(), 2.71);
    assertEquals(2.71, result, 1e-9);
  }

  @Test
  void taskRuntime_invokeConstructedVoid_unknownClass_swallowsThrowable() {
    TaskRuntime.invokeConstructedVoid(
        "iped.engine.task.NonExistentClass", String.class, "arg", "method", String.class, "param");
  }

  @Test
  void taskRuntime_privateConstructor_isInvocableViaReflection() throws Exception {
    Constructor<TaskRuntime> c = TaskRuntime.class.getDeclaredConstructor();
    c.setAccessible(true);
    assertNotNull(c.newInstance());
  }

  // ── ExportFileTaskRuntime (fallback paths when target class is absent) ───

  @Test
  void exportFileTaskRuntime_getExtractDirName_fallsBackToExtracted() {
    // Messages class may or may not be on the test classpath; either way
    // the method must return a non-null, non-empty string.
    String name = ExportFileTaskRuntime.getExtractDirName();
    assertNotNull(name);
    assertEquals(false, name.isEmpty());
  }

  @Test
  void exportFileTaskRuntime_getItemsExtracted_fallsBackToZero() {
    // ExportFileTask is not on the test classpath, so result must be 0.
    assertEquals(0, ExportFileTaskRuntime.getItemsExtracted());
  }

  @Test
  void exportFileTaskRuntime_commitStorage_unknownClass_swallowsThrowable() {
    ExportFileTaskRuntime.commitStorage(new java.io.File("/nonexistent/output"));
  }

  @Test
  void exportFileTaskRuntime_privateConstructor_isInvocableViaReflection() throws Exception {
    Constructor<ExportFileTaskRuntime> c = ExportFileTaskRuntime.class.getDeclaredConstructor();
    c.setAccessible(true);
    assertNotNull(c.newInstance());
  }
}
