package iped.engine.graph;

import java.io.File;
import java.util.concurrent.ConcurrentHashMap;

public class GraphServiceFactoryImpl implements GraphServiceFactory {

    private static GraphServiceFactory instance;
    private static final ConcurrentHashMap<File, GraphService> serviceInstances = new ConcurrentHashMap<>();

    @Override
    public synchronized GraphService getGraphService() {
        return getGraphService(null);
    }

    public synchronized GraphService getGraphService(File graphDbFolder) {
        if (graphDbFolder == null) {
            return new GraphServiceImpl();
        }

        return serviceInstances.computeIfAbsent(graphDbFolder, folder -> new GraphServiceImpl());
    }

    public synchronized static GraphServiceFactory getInstance() {
        if (instance == null) {
            instance = new GraphServiceFactoryImpl();
        }
        return instance;
    }

}
