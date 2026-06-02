package iped.engine.webapi.spi;

import iped.data.IIPEDSource;

import java.util.List;

public interface SourceCatalogService {
    void init(String urlToAskSources) throws Exception;

    List<SourceDescriptor> listSources() throws Exception;

    void addSource(SourceDescriptor source);

    SourceDescriptor getSource(String sourceId) throws Exception;

    IIPEDSource getSourceHandle(String sourceId);

    String getSourceStringId(int sourceId);

    int getSourceIntId(String sourceId);
}
