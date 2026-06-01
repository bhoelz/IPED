package iped.engine.webapi.spi;

import java.util.List;

public interface SearchService {
    List<DocRef> search(String query, String sourceId) throws Exception;
}
