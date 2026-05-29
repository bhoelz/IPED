package iped.engine.config;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

/**
 * Tests to verify ProcessingOrchestratorConfig manages configuration correctly.
 */
public class ProcessingOrchestratorConfigTest {

    @Test
    void testDefaultConfiguration() {
        ProcessingOrchestratorConfig config = new ProcessingOrchestratorConfig();

        assertTrue(config.getMaxConcurrentCases() > 0, "Max concurrent cases should be set");
        assertTrue(config.getMaxMemoryPerCase() > 0, "Max memory per case should be set");
        assertTrue(config.getSharedThreadPoolSize() > 0, "Thread pool size should be set");
    }

    @Test
    void testConfigurationSettersAndGetters() {
        ProcessingOrchestratorConfig config = new ProcessingOrchestratorConfig();

        config.setMaxConcurrentCases(4);
        assertEquals(4, config.getMaxConcurrentCases());

        config.setMaxMemoryPerCase(1000000);
        assertEquals(1000000, config.getMaxMemoryPerCase());

        config.setSharedThreadPoolSize(8);
        assertEquals(8, config.getSharedThreadPoolSize());
    }

    @Test
    void testConfigurationReflectsSystemDefaults() {
        ProcessingOrchestratorConfig config = new ProcessingOrchestratorConfig();
        int expectedCores = Runtime.getRuntime().availableProcessors();

        assertEquals(expectedCores, config.getMaxConcurrentCases(),
                "Default concurrent cases should match available processors");
        assertEquals(expectedCores, config.getSharedThreadPoolSize(),
                "Default thread pool size should match available processors");
    }
}
