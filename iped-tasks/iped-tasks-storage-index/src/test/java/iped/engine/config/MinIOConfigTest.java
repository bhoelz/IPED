package iped.engine.config;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class MinIOConfigTest {

    @Test
    void getTaskEnableProperty_isNonBlank() {
        assertFalse(new MinIOConfig().getTaskEnableProperty().isBlank());
    }

    @Test
    void getTaskConfigFileName_isNonBlank() {
        assertFalse(new MinIOConfig().getTaskConfigFileName().isBlank());
    }

    @Test
    void defaults_hostNull() {
        assertNull(new MinIOConfig().getHost());
    }

    @Test
    void defaults_portNull() {
        assertNull(new MinIOConfig().getPort());
    }

    @Test
    void defaults_updateRefsToMinIOFalse() {
        assertFalse(new MinIOConfig().isToUpdateRefsToMinIO());
    }

    @Test
    void setAndGetZipFilesMaxSize() {
        MinIOConfig cfg = new MinIOConfig();
        cfg.setZipFilesMaxSize(1024L);
        assertEquals(1024L, cfg.getZipFilesMaxSize());
    }

    @Test
    void getHostAndPort_whenBothNull_thenNull() {
        MinIOConfig cfg = new MinIOConfig();
        // host is null; getHostAndPort() returns host (null)
        assertNull(cfg.getHostAndPort());
    }

    @Test
    void getConfiguration_whenNew_thenNotNull() {
        assertNotNull(new MinIOConfig().getConfiguration());
    }
}
