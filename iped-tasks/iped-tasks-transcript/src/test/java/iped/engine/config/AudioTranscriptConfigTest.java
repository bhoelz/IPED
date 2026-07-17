package iped.engine.config;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class AudioTranscriptConfigTest {

  @Test
  void getTaskEnableProperty_isNonBlank() {
    assertFalse(new AudioTranscriptConfig().getTaskEnableProperty().isBlank());
  }

  @Test
  void getTaskConfigFileName_isNonBlank() {
    assertFalse(new AudioTranscriptConfig().getTaskConfigFileName().isBlank());
  }

  @Test
  void publicConstants_areNonBlank() {
    assertFalse(AudioTranscriptConfig.CONF_FILE.isBlank());
    assertFalse(AudioTranscriptConfig.HUGGING_FACE_MODEL.isBlank());
    assertFalse(AudioTranscriptConfig.WHISPER_MODEL.isBlank());
    assertFalse(AudioTranscriptConfig.WAV2VEC2_SERVICE.isBlank());
    assertFalse(AudioTranscriptConfig.REMOTE_SERVICE.isBlank());
  }

  @Test
  void defaults_minTimeout() {
    assertEquals(180, new AudioTranscriptConfig().getMinTimeout());
  }

  @Test
  void defaults_timeoutPerSec() {
    assertEquals(3, new AudioTranscriptConfig().getTimeoutPerSec());
  }

  @Test
  void defaults_minWordScore() {
    assertEquals(0.7f, new AudioTranscriptConfig().getMinWordScore(), 0.001f);
  }

  @Test
  void defaults_skipKnownFilesTrue() {
    assertTrue(new AudioTranscriptConfig().getSkipKnownFiles());
  }

  @Test
  void defaults_precision() {
    assertEquals("int8", new AudioTranscriptConfig().getPrecision());
  }

  @Test
  void defaults_batchSize() {
    assertEquals(1, new AudioTranscriptConfig().getBatchSize());
  }

  @Test
  void defaults_device() {
    assertEquals("cpu", new AudioTranscriptConfig().getDevice());
  }

  @Test
  void defaults_languagesEmpty() {
    assertTrue(new AudioTranscriptConfig().getLanguages().isEmpty());
  }

  @Test
  void defaults_mimesToProcessEmpty() {
    assertTrue(new AudioTranscriptConfig().getMimesToProcess().isEmpty());
  }

  @Test
  void defaults_classNameNull() {
    assertNull(new AudioTranscriptConfig().getClassName());
  }

  @Test
  void setClassName_thenGetClassNameReturnsIt() {
    AudioTranscriptConfig cfg = new AudioTranscriptConfig();
    cfg.setClassName("com.example.Impl");
    assertEquals("com.example.Impl", cfg.getClassName());
  }

  @Test
  void getConfiguration_whenNew_thenNotNull() {
    assertNotNull(new AudioTranscriptConfig().getConfiguration());
  }
}
