package iped.engine.webapi.spi;

import java.util.List;

import iped.data.IIPEDSource;

public interface SourceCatalogService {
    void init(String urlToAskSources) throws Exception;

    List<SourceDescriptor> listSources() throws Exception;

    void addSource(SourceDescriptor source);

    SourceDescriptor getSource(String sourceId) throws Exception;

    IIPEDSource getSourceHandle(String sourceId);

    String getSourceStringId(int sourceId);

    int getSourceIntId(String sourceId);
}
