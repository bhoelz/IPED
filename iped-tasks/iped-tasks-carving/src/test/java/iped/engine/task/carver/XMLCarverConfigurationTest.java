package iped.engine.task.carver;

import static org.junit.jupiter.api.Assertions.*;

import iped.carvers.api.CarverType;
import org.junit.jupiter.api.Test;

class XMLCarverConfigurationTest {

  @Test
  void constructor_whenNew_thenNotNull() {
    assertNotNull(new XMLCarverConfiguration());
  }

  @Test
  void getCarverTypes_whenNew_thenEmptyOrNull() {
    XMLCarverConfiguration cfg = new XMLCarverConfiguration();
    // No carvers loaded — the array should be null or empty
    CarverType[] types = cfg.getCarverTypes();
    assertTrue(types == null || types.length == 0);
  }

  @Test
  void isToIgnoreCorrupted_defaultsToTrue() {
    XMLCarverConfiguration cfg = new XMLCarverConfiguration();
    assertTrue(cfg.isToIgnoreCorrupted());
  }

  @Test
  void isToProcess_whenNew_thenReturnsFalseOrHandlesGracefully() {
    XMLCarverConfiguration cfg = new XMLCarverConfiguration();
    // Before loading a config, isToProcess should not throw
    org.apache.tika.mime.MediaType mt = org.apache.tika.mime.MediaType.image("jpeg");
    assertDoesNotThrow(() -> cfg.isToProcess(mt));
  }

  @Test
  void isToCarve_whenNew_thenReturnsFalseOrHandlesGracefully() {
    XMLCarverConfiguration cfg = new XMLCarverConfiguration();
    org.apache.tika.mime.MediaType mt = org.apache.tika.mime.MediaType.image("png");
    assertDoesNotThrow(() -> cfg.isToCarve(mt));
  }
}
