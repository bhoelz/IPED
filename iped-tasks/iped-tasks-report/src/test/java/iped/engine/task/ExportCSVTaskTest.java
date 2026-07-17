package iped.engine.task;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

/**
 * Tests for ExportCSVTask that are exercisable without the full AbstractTask lifecycle (no
 * ConfigurationManager, CaseData, or output directory needed).
 *
 * <p>The full CSV-writing logic is covered by integration tests that run the complete IPED
 * pipeline. Here we focus on the task contract methods and constants that can be verified in
 * isolation.
 */
class ExportCSVTaskTest {

  @Test
  void processIgnoredItem_returnsTrue() {
    ExportCSVTask task = new ExportCSVTask();
    assertTrue(
        task.processIgnoredItem(),
        "ExportCSVTask must process ignored items (duplicates / known-hash) to include them in the CSV");
  }

  @Test
  void isEnabled_defaultFalse() {
    // The static exportFileProps field is false unless init() has been called
    // with a ConfigurationManager that enables it.
    ExportCSVTask task = new ExportCSVTask();
    assertFalse(task.isEnabled());
  }

  @Test
  void getConfigurables_returnsNonNullList() {
    ExportCSVTask task = new ExportCSVTask();
    assertNotNull(task.getConfigurables());
  }

  @Test
  void getConfigurables_hasOneEntry() {
    ExportCSVTask task = new ExportCSVTask();
    assertEquals(1, task.getConfigurables().size());
  }

  @Test
  void getConfigurables_firstEntry_isEnableTaskProperty() {
    ExportCSVTask task = new ExportCSVTask();
    Object configurable = task.getConfigurables().get(0);
    assertNotNull(configurable);
    // The configurable must be an EnableTaskProperty (or a subtype)
    assertTrue(
        configurable instanceof iped.engine.config.EnableTaskProperty,
        "Expected EnableTaskProperty but got: " + configurable.getClass().getSimpleName());
  }
}
