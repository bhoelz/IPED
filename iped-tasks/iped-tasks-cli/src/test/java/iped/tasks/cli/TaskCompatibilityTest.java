package iped.tasks.cli;

import static iped.tasks.cli.TaskCompatibility.Status.COMPATIBLE;
import static iped.tasks.cli.TaskCompatibility.Status.KNOWN_UNSUPPORTED;
import static iped.tasks.cli.TaskCompatibility.Status.UNCLEAR;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class TaskCompatibilityTest {

  @Test
  void classifiesCaseDependentTaskAsUnsupported() {
    TaskCompatibility.Classification c =
        TaskCompatibility.classify("iped.engine.task.DuplicateTask");
    assertEquals(KNOWN_UNSUPPORTED, c.status());
    assertTrue(c.reason().contains("case"));
  }

  @Test
  void classifiesChildCreatingTaskAsUnsupported() {
    TaskCompatibility.Classification c = TaskCompatibility.classify("iped.engine.task.ParsingTask");
    assertEquals(KNOWN_UNSUPPORTED, c.status());
    assertTrue(c.reason().contains("child"));
  }

  @Test
  void classifiesKnownSafeTaskAsCompatible() {
    TaskCompatibility.Classification c = TaskCompatibility.classify("iped.engine.task.HashTask");
    assertEquals(COMPATIBLE, c.status());
  }

  @Test
  void classifiesUnknownTaskAsUnclear() {
    TaskCompatibility.Classification c =
        TaskCompatibility.classify("iped.engine.task.SomeTaskNeverReviewed");
    assertEquals(UNCLEAR, c.status());
  }

  @Test
  void classifiesNullGuardedThumbAndPreviewAndTranscriptTasksAsCompatible() {
    for (String className :
        new String[] {
          "iped.engine.task.ThumbTask",
          "iped.engine.task.DocThumbTask",
          "iped.engine.task.ImageThumbTask",
          "iped.engine.task.MakePreviewTask",
          "iped.engine.task.transcript.AudioTranscriptTask"
        }) {
      TaskCompatibility.Classification c = TaskCompatibility.classify(className);
      assertEquals(COMPATIBLE, c.status(), className + " should be COMPATIBLE");
    }
  }

  @Test
  void classifiesCarvingFamilyAsUnclearWithExtractChildrenNote() {
    for (String className : TaskCompatibility.CARVING_FAMILY) {
      TaskCompatibility.Classification c = TaskCompatibility.classify(className);
      assertEquals(UNCLEAR, c.status(), className + " should be UNCLEAR");
      assertTrue(
          c.reason().contains("--extract-children-to"),
          className + " reason should mention --extract-children-to");
    }
  }

  @Test
  void classifiesScriptAndPythonAndRemoteClassifierTasksWithSpecificNotes() {
    TaskCompatibility.Classification script =
        TaskCompatibility.classify("iped.engine.task.ScriptTask");
    assertEquals(UNCLEAR, script.status());
    assertTrue(script.reason().contains("script"));

    TaskCompatibility.Classification python =
        TaskCompatibility.classify("iped.engine.task.PythonTask");
    assertEquals(UNCLEAR, python.status());
    assertTrue(python.reason().contains("script"));

    TaskCompatibility.Classification remote =
        TaskCompatibility.classify("iped.engine.task.RemoteImageClassifierTask");
    assertEquals(UNCLEAR, remote.status());
    assertTrue(remote.reason().contains("reachable"));
  }
}
