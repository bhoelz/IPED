package iped.engine.graph;

import java.io.File;

public interface GraphServiceFactory {

    GraphService getGraphService();

    GraphService getGraphService(File graphDbFolder);

}
