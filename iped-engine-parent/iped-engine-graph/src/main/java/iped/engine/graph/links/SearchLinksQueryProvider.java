package iped.engine.graph.links;

import iped.io.URLUtil;
import lombok.extern.slf4j.Slf4j;

import java.net.URL;
import java.util.*;

@Slf4j
public class SearchLinksQueryProvider {


    private static final SearchLinksQueryProvider INSTANCE = new SearchLinksQueryProvider();

    private Map<String, SearchLinksQuery> queries = new TreeMap<>();

    private SearchLinksQueryProvider() {
        super();
        init();
    }

    private void init() {
        ClassLoader cl = SearchLinksQuery.class.getClassLoader();
        init(cl);
    }

    private void init(ClassLoader classLoader) {
        ServiceLoader<SearchLinksQuery> loader = ServiceLoader.load(SearchLinksQuery.class, classLoader);
        Iterator<SearchLinksQuery> iterator = loader.iterator();
        while (iterator.hasNext()) {
            SearchLinksQuery query = iterator.next();
            String queryName = query.getQueryName();

            SearchLinksQuery previous = queries.put(queryName, query);
            if (previous != null) {

                String first = getLocation(previous);
                String second = getLocation(query);

                throw new IllegalStateException(
                        "Multiple query registered with name " + queryName + " (" + first + " and " + second + ")");
            }

            log.info("Query " + query.getClass().getName() + " found with name " + queryName);
        }
    }

    private String getLocation(SearchLinksQuery query) {
        try {
            URL location = URLUtil.getURL(query.getClass());
            return location.toString();
        } catch (SecurityException e) {
            return null;
        }
    }

    public static SearchLinksQueryProvider get() {
        return INSTANCE;
    }

    public Collection<SearchLinksQuery> getQueries() {
        return queries.values();
    }

    public SearchLinksQuery getQuery(String name) {
        return queries.get(name);
    }

}
