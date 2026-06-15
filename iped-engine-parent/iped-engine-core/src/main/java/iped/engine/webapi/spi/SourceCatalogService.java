package iped.engine.webapi.spi;

import iped.data.IIPEDSource;

import java.util.List;

public interface SourceCatalogService {
    void init(String urlToAskSources) throws Exception;

    List<SourceDescriptor> listSources() throws Exception;

    void addSource(SourceDescriptor source);

    SourceDescriptor getSource(String sourceId) throws Exception;

    IIPEDSource getSourceHandle(String sourceId);

    /**
     * Closes and removes a case from the catalog.
     * The source is no longer accessible after this call.
     * In-flight queries against this source may fail with a runtime exception.
     *
     * @throws IllegalArgumentException if {@code sourceId} is unknown
     */
    void removeSource(String sourceId) throws Exception;

    String getSourceStringId(int sourceId);

    int getSourceIntId(String sourceId);
}
