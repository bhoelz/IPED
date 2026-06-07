package iped.engine.task;

import iped.engine.config.MakePreviewConfig;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for MakePreviewTask that are exercisable without the full AbstractTask
 * lifecycle (no ConfigurationManager, StandardParser, or case directory needed).
 */
class MakePreviewTaskTest {

    @Test
    void getConfigurables_returnsNonNullList() {
        MakePreviewTask task = new MakePreviewTask();
        assertNotNull(task.getConfigurables());
    }

    @Test
    void getConfigurables_hasOneEntry() {
        MakePreviewTask task = new MakePreviewTask();
        assertEquals(1, task.getConfigurables().size());
    }

    @Test
    void getConfigurables_firstEntry_isMakePreviewConfig() {
        MakePreviewTask task = new MakePreviewTask();
        Object configurable = task.getConfigurables().get(0);
        assertNotNull(configurable);
        assertInstanceOf(MakePreviewConfig.class, configurable,
                "getConfigurables() should return a MakePreviewConfig instance");
    }

    @Test
    void constructor_doesNotThrow() {
        assertDoesNotThrow(MakePreviewTask::new);
    }

    @Test
    void makePreviewConfig_defaultSupportedMimes_nonNull() {
        // Verify that the config returned by getConfigurables() has a usable state
        MakePreviewTask task = new MakePreviewTask();
        MakePreviewConfig config = (MakePreviewConfig) task.getConfigurables().get(0);
        // getSupportedMimes() returns the set of supported media types — may be
        // empty before loadConfig() is called but must not be null
        assertNotNull(config.getSupportedMimes());
    }
}
