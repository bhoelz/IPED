package iped.engine.config;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

class MakePreviewConfigTest {

  @Test
  void getTaskEnableProperty_isNonBlank() {
    assertFalse(new MakePreviewConfig().getTaskEnableProperty().isBlank());
  }

  @Test
  void getTaskConfigFileName_isNonBlank() {
    assertFalse(new MakePreviewConfig().getTaskConfigFileName().isBlank());
  }

  @Test
  void getSupportedMimes_whenNew_thenEmpty() {
    assertTrue(new MakePreviewConfig().getSupportedMimes().isEmpty());
  }

  @Test
  void getSupportedMimesWithLinks_whenNew_thenEmpty() {
    assertTrue(new MakePreviewConfig().getSupportedMimesWithLinks().isEmpty());
  }

  @Test
  void getConfiguration_returnsTwoSets() {
    MakePreviewConfig cfg = new MakePreviewConfig();
    List<Set<String>> config = cfg.getConfiguration();
    assertNotNull(config);
    assertEquals(2, config.size());
  }

  @Test
  void setConfiguration_thenGetConfigurationReturnsIt() {
    MakePreviewConfig cfg = new MakePreviewConfig();
    Set<String> mimes = new HashSet<>(Set.of("text/plain", "application/pdf"));
    Set<String> mimesWithLinks = new HashSet<>(Set.of("text/html"));
    cfg.setConfiguration(Arrays.asList(mimes, mimesWithLinks));

    List<Set<String>> result = cfg.getConfiguration();
    assertEquals(mimes, result.get(0));
    assertEquals(mimesWithLinks, result.get(1));
  }

  @Test
  void setConfiguration_thenSupportedMimesUpdated() {
    MakePreviewConfig cfg = new MakePreviewConfig();
    Set<String> mimes = new HashSet<>(Set.of("image/png"));
    Set<String> mimesWithLinks = new HashSet<>();
    cfg.setConfiguration(Arrays.asList(mimes, mimesWithLinks));

    assertTrue(cfg.getSupportedMimes().contains("image/png"));
    assertTrue(cfg.getSupportedMimesWithLinks().isEmpty());
  }
}
