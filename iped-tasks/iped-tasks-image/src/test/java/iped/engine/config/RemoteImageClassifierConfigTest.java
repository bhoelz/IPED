package iped.engine.config;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class RemoteImageClassifierConfigTest {

  @Test
  void getTaskEnableProperty_isNonBlank() {
    assertFalse(new RemoteImageClassifierConfig().getTaskEnableProperty().isBlank());
  }

  @Test
  void getTaskConfigFileName_isNonBlank() {
    assertFalse(new RemoteImageClassifierConfig().getTaskConfigFileName().isBlank());
  }

  @Test
  void defaults_urlIsNull() {
    assertNull(new RemoteImageClassifierConfig().getUrl());
  }

  @Test
  void defaults_batchSize() {
    assertEquals(50, new RemoteImageClassifierConfig().getBatchSize());
  }

  @Test
  void defaults_labelingThreshold() {
    assertEquals(60.0, new RemoteImageClassifierConfig().getLabelingThreshold(), 0.001);
  }

  @Test
  void defaults_skipSize() {
    assertEquals(0, new RemoteImageClassifierConfig().getSkipSize());
  }

  @Test
  void defaults_skipDimension() {
    assertEquals(0, new RemoteImageClassifierConfig().getSkipDimension());
  }

  @Test
  void defaults_skipHashDBFilesTrue() {
    assertTrue(new RemoteImageClassifierConfig().isSkipHashDBFiles());
  }

  @Test
  void defaults_validateSSLFalse() {
    assertFalse(new RemoteImageClassifierConfig().isValidateSSL());
  }

  @Test
  void defaults_connectTimeout() {
    assertEquals(30_000, new RemoteImageClassifierConfig().getConnectTimeout());
  }

  @Test
  void defaults_socketTimeout() {
    assertEquals(3 * 60 * 1000, new RemoteImageClassifierConfig().getSocketTimeout());
  }

  @Test
  void getConfiguration_whenNew_thenNotNull() {
    assertNotNull(new RemoteImageClassifierConfig().getConfiguration());
  }
}
