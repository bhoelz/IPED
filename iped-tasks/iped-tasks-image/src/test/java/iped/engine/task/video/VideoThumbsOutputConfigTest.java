package iped.engine.task.video;

import iped.engine.config.VideoThumbsConfig;
import org.junit.jupiter.api.Test;

import java.io.File;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class VideoThumbsOutputConfigTest {

    private static VideoThumbsConfig defaultVideoConfig() {
        return new VideoThumbsConfig();
    }

    @Test
    void constructor_setsOutFile() {
        File out = new File("thumb.jpg");
        VideoThumbsOutputConfig cfg = new VideoThumbsOutputConfig(out, defaultVideoConfig(), 2);
        assertEquals(out, cfg.getOutFile());
    }

    @Test
    void constructor_setsBorder() {
        VideoThumbsOutputConfig cfg = new VideoThumbsOutputConfig(new File("x"), defaultVideoConfig(), 5);
        assertEquals(5, cfg.getBorder());
    }

    @Test
    void constructor_setsRowsAndColumnsFromVideoConfig() {
        VideoThumbsConfig vcfg = defaultVideoConfig();
        VideoThumbsOutputConfig cfg = new VideoThumbsOutputConfig(new File("x"), vcfg, 0);
        assertEquals(vcfg.getRows(), cfg.getRows());
        assertEquals(vcfg.getColumns(), cfg.getColumns());
    }

    @Test
    void getThumbSize_whenNotSet_usesVideoConfigSize() {
        VideoThumbsConfig vcfg = defaultVideoConfig();
        VideoThumbsOutputConfig cfg = new VideoThumbsOutputConfig(new File("x"), vcfg, 0);
        assertEquals(vcfg.getSize(), cfg.getThumbSize());
    }

    @Test
    void setThumbSize_overridesVideoConfigSize() {
        VideoThumbsOutputConfig cfg = new VideoThumbsOutputConfig(new File("x"), defaultVideoConfig(), 0);
        cfg.setThumbSize(100);
        assertEquals(100, cfg.getThumbSize());
    }

    @Test
    void setters_roundTrip() {
        VideoThumbsOutputConfig cfg = new VideoThumbsOutputConfig(new File("x"), defaultVideoConfig(), 0);
        File newFile = new File("new.jpg");
        cfg.setOutFile(newFile);
        cfg.setRows(4);
        cfg.setColumns(5);
        cfg.setBorder(3);

        assertEquals(newFile, cfg.getOutFile());
        assertEquals(4, cfg.getRows());
        assertEquals(5, cfg.getColumns());
        assertEquals(3, cfg.getBorder());
    }

    @Test
    void toString_containsFieldNames() {
        VideoThumbsOutputConfig cfg = new VideoThumbsOutputConfig(new File("test.jpg"), defaultVideoConfig(), 1);
        String str = cfg.toString();
        assertTrue(str.contains("VideoThumbsOutputConfig"));
        assertTrue(str.contains("thumbSize"));
    }
}
