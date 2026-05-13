package iped.engine.webapi.spi;

import java.util.List;

public interface SelectionService {
    List<DocRef> getSelected() throws Exception;

    void add(List<DocRef> docs);

    void remove(List<DocRef> docs);
}
