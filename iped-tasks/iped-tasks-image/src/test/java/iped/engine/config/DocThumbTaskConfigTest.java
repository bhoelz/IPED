package iped.engine.config;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class DocThumbTaskConfigTest {

  @Test
  void getTaskEnableProperty_isNonBlank() {
    DocThumbTaskConfig cfg = new DocThumbTaskConfig();
    assertFalse(cfg.getTaskEnableProperty().isBlank());
  }

  @Test
  void getTaskConfigFileName_isNonBlank() {
    DocThumbTaskConfig cfg = new DocThumbTaskConfig();
    assertFalse(cfg.getTaskConfigFileName().isBlank());
  }

  @Test
  void defaults_pdfTimeout() {
    assertEquals(60, new DocThumbTaskConfig().getPdfTimeout());
  }

  @Test
  void defaults_loTimeout() {
    assertEquals(180, new DocThumbTaskConfig().getLoTimeout());
  }

  @Test
  void defaults_timeoutIncPerMB() {
    assertEquals(2, new DocThumbTaskConfig().getTimeoutIncPerMB());
  }

  @Test
  void defaults_thumbSize() {
    assertEquals(480, new DocThumbTaskConfig().getThumbSize());
  }

  @Test
  void defaults_maxPdfExternalMemory() {
    assertEquals(256, new DocThumbTaskConfig().getMaxPdfExternalMemory());
  }

  @Test
  void defaults_pdfEnabledFalse() {
    assertFalse(new DocThumbTaskConfig().isPdfEnabled());
  }

  @Test
  void defaults_loEnabledFalse() {
    assertFalse(new DocThumbTaskConfig().isLoEnabled());
  }

  @Test
  void defaults_externalPdfConversionFalse() {
    assertFalse(new DocThumbTaskConfig().isExternalPdfConversion());
  }

  @Test
  void getConfiguration_whenNew_thenNotNull() {
    assertNotNull(new DocThumbTaskConfig().getConfiguration());
  }
}
