package iped.engine.graph;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class GraphConstantsTest {

  @Test
  void dbName_isNonBlank() {
    assertNotNull(GraphConstants.DB_NAME);
    assertFalse(GraphConstants.DB_NAME.isBlank());
    assertEquals("graph.db", GraphConstants.DB_NAME);
  }

  @Test
  void dbHomeDir_isNonBlank() {
    assertNotNull(GraphConstants.DB_HOME_DIR);
    assertFalse(GraphConstants.DB_HOME_DIR.isBlank());
    assertEquals("neo4j", GraphConstants.DB_HOME_DIR);
  }

  @Test
  void dbDataDir_isNonBlank() {
    assertEquals("data", GraphConstants.DB_DATA_DIR);
  }

  @Test
  void dbDataPath_combinesHomeDirAndDataDir() {
    assertEquals(
        GraphConstants.DB_HOME_DIR + "/" + GraphConstants.DB_DATA_DIR, GraphConstants.DB_DATA_PATH);
    assertEquals("neo4j/data", GraphConstants.DB_DATA_PATH);
  }

  @Test
  void csvsDir_isNonBlank() {
    assertEquals("csv", GraphConstants.CSVS_DIR);
  }

  @Test
  void csvsPath_combinesHomeDirAndCsvsDir() {
    assertEquals(
        GraphConstants.DB_HOME_DIR + "/" + GraphConstants.CSVS_DIR, GraphConstants.CSVS_PATH);
    assertEquals("neo4j/csv", GraphConstants.CSVS_PATH);
  }

  @Test
  void relationshipId_isNonBlank() {
    assertNotNull(GraphConstants.RELATIONSHIP_ID);
    assertFalse(GraphConstants.RELATIONSHIP_ID.isBlank());
  }

  @Test
  void relationshipSource_isNonBlank() {
    assertNotNull(GraphConstants.RELATIONSHIP_SOURCE);
    assertFalse(GraphConstants.RELATIONSHIP_SOURCE.isBlank());
  }
}
