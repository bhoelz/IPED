package iped.engine.graph.links;

import iped.engine.graph.PathQueryListener;
import org.neo4j.graphdb.GraphDatabaseService;

public interface SearchLinksQuery {

    void search(String start, String end, GraphDatabaseService graphDB, PathQueryListener listener);

    String getQueryName();

    String getLabel();
}
