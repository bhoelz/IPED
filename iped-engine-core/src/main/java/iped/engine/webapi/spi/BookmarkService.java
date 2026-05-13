package iped.engine.webapi.spi;

import java.util.List;
import java.util.Set;

public interface BookmarkService {
    Set<String> listBookmarks();

    List<DocRef> listBookmarkDocs(String bookmark) throws Exception;

    void add(String bookmark, List<DocRef> docs);

    void remove(String bookmark, List<DocRef> docs);

    void create(String bookmark);

    void delete(String bookmark);

    void rename(String oldBookmark, String newBookmark);
}
