package iped.engine.graph;

public final class GraphConstants {

  private GraphConstants() {}

  public static final String DB_NAME = "graph.db";
  public static final String DB_HOME_DIR = "neo4j";
  public static final String DB_DATA_DIR = "data";
  public static final String DB_DATA_PATH = DB_HOME_DIR + "/" + DB_DATA_DIR;
  public static final String CSVS_DIR = "csv";
  public static final String CSVS_PATH = DB_HOME_DIR + "/" + CSVS_DIR;
  public static final String RELATIONSHIP_ID = "relId";
  public static final String RELATIONSHIP_SOURCE = "dataSource";
}
