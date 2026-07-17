package iped.engine.graph;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class GraphTaskConfigTest {

  @Test
  void enableParam_isCorrect() {
    assertEquals("enableGraphGeneration", GraphTaskConfig.ENABLE_PARAM);
  }

  @Test
  void configFile_isCorrect() {
    assertEquals("GraphConfig.json", GraphTaskConfig.CONFIG_FILE);
  }

  @Test
  void getTaskEnableProperty_matchesConstant() {
    GraphTaskConfig cfg = new GraphTaskConfig();
    assertEquals(GraphTaskConfig.ENABLE_PARAM, cfg.getTaskEnableProperty());
  }

  @Test
  void getTaskConfigFileName_matchesConstant() {
    GraphTaskConfig cfg = new GraphTaskConfig();
    assertEquals(GraphTaskConfig.CONFIG_FILE, cfg.getTaskConfigFileName());
  }

  @Test
  void getConfiguration_initiallyNull() {
    GraphTaskConfig cfg = new GraphTaskConfig();
    assertNull(cfg.getConfiguration());
  }

  @Test
  void setConfiguration_roundTrip() {
    GraphTaskConfig cfg = new GraphTaskConfig();
    GraphConfiguration graphConfig = new GraphConfiguration();
    cfg.setConfiguration(graphConfig);
    assertSame(graphConfig, cfg.getConfiguration());
  }

  @Test
  void setConfiguration_withNull_returnsNull() {
    GraphTaskConfig cfg = new GraphTaskConfig();
    cfg.setConfiguration(null);
    assertNull(cfg.getConfiguration());
  }

  @Test
  void enableParam_isNonBlank() {
    assertFalse(GraphTaskConfig.ENABLE_PARAM.isBlank());
  }

  @Test
  void configFile_endsWithJson() {
    assertTrue(GraphTaskConfig.CONFIG_FILE.endsWith(".json"));
  }
}
