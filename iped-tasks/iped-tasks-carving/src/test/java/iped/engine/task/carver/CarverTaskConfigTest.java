package iped.engine.task.carver;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CarverTaskConfigTest {

    @Test
    void constants_areNonBlank() {
        assertFalse(CarverTaskConfig.ENABLE_PARAM.isBlank());
        assertFalse(CarverTaskConfig.GLOBAL_CARVER_CONFIG.isBlank());
        assertFalse(CarverTaskConfig.CARVER_CONFIG_PREFIX.isBlank());
        assertFalse(CarverTaskConfig.CARVER_CONFIG_SUFFIX.isBlank());
    }

    @Test
    void getTaskEnableProperty_returnsEnableParam() {
        CarverTaskConfig cfg = new CarverTaskConfig();
        assertEquals(CarverTaskConfig.ENABLE_PARAM, cfg.getTaskEnableProperty());
    }

    @Test
    void getTaskConfigFileName_returnsGlobalCarverConfig() {
        CarverTaskConfig cfg = new CarverTaskConfig();
        assertEquals(CarverTaskConfig.GLOBAL_CARVER_CONFIG, cfg.getTaskConfigFileName());
    }

    @Test
    void getConfiguration_whenNew_thenNotNull() {
        CarverTaskConfig cfg = new CarverTaskConfig();
        assertNotNull(cfg.getConfiguration());
    }

    @Test
    void setConfiguration_thenGetConfigurationReturnsIt() {
        CarverTaskConfig cfg = new CarverTaskConfig();
        XMLCarverConfiguration newConfig = new XMLCarverConfiguration();
        cfg.setConfiguration(newConfig);
        assertSame(newConfig, cfg.getConfiguration());
    }

    @Test
    void getResourceLookupFilter_whenNew_thenNotNull() {
        CarverTaskConfig cfg = new CarverTaskConfig();
        assertNotNull(cfg.getResourceLookupFilter());
    }
}
