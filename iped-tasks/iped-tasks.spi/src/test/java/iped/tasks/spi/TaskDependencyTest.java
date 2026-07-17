package iped.tasks.spi;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

public class TaskDependencyTest {

  @Test
  public void requiresCreatesMandatoryDependency() {
    TaskDependency dep = TaskDependency.requires("hash");
    assertEquals("hash", dep.taskId());
    assertEquals(TaskDependencyType.REQUIRES, dep.type());
    assertFalse(dep.optional());
  }

  @Test
  public void optionalRequiresCreatesOptionalDependency() {
    TaskDependency dep = TaskDependency.optionalRequires("ocr");
    assertEquals(TaskDependencyType.REQUIRES, dep.type());
    assertTrue(dep.optional());
  }

  @Test
  public void beforeAndAfterCreateOrderingDependencies() {
    assertEquals(TaskDependencyType.BEFORE, TaskDependency.before("index").type());
    assertEquals(TaskDependencyType.AFTER, TaskDependency.after("hash").type());
    assertFalse(TaskDependency.before("index").optional());
  }

  @Test
  public void rejectsNullOrBlankTaskId() {
    assertThrows(IllegalArgumentException.class, () -> TaskDependency.requires(null));
    assertThrows(IllegalArgumentException.class, () -> TaskDependency.requires(""));
    assertThrows(IllegalArgumentException.class, () -> TaskDependency.requires("   "));
  }

  @Test
  public void rejectsNullType() {
    assertThrows(IllegalArgumentException.class, () -> new TaskDependency("hash", null, false));
  }
}
