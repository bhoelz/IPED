package iped.engine.webapi;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.tika.metadata.Metadata;
import org.apache.tika.parser.ParseContext;
import org.apache.tika.sax.ToTextContentHandler;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;
import org.xml.sax.ContentHandler;

import iped.data.IIPEDSource;
import iped.data.IItem;
import iped.data.IItemId;
import iped.data.IMultiBookmarks;
import iped.engine.config.Configuration;
import iped.engine.config.ConfigurationManager;
import iped.engine.data.IPEDMultiSource;
import iped.engine.data.IPEDSource;
import iped.engine.data.ItemId;
import iped.engine.data.ItemIdSet;
import iped.engine.search.IPEDSearcher;
import iped.engine.task.ParsingTaskContextFactory;
import iped.engine.task.ParsingTaskSupport;
import iped.engine.webapi.spi.BookmarkService;
import iped.engine.webapi.spi.DocRef;
import iped.engine.webapi.spi.SearchService;
import iped.engine.webapi.spi.SelectionService;
import iped.engine.webapi.spi.SourceCatalogService;
import iped.engine.webapi.spi.SourceDescriptor;
import iped.engine.webapi.spi.TextService;
import iped.engine.webapi.spi.WebApiServices;
import iped.engine.webapi.spi.WebApiServicesFactory;
import iped.parsers.standard.StandardParser;
import iped.search.IIPEDSearcher;
import iped.search.IMultiSearchResult;
import iped.search.SearchResult;

public class EngineWebApiServicesFactory implements WebApiServicesFactory {
    @Override
    public WebApiServices create() {
        return new EngineWebApiServices();
    }

    static class EngineWebApiServices implements WebApiServices {
        private final EngineSourceCatalogService sources = new EngineSourceCatalogService();
        private final SearchService search = new EngineSearchService(sources);
        private final SelectionService selection = new EngineSelectionService(sources);
        private final BookmarkService bookmarks = new EngineBookmarkService(sources);
        private final TextService text = new EngineTextService(sources);

        @Override
        public SourceCatalogService sources() {
            return sources;
        }

        @Override
        public SearchService search() {
            return search;
        }

        @Override
        public SelectionService selection() {
            return selection;
        }

        @Override
        public BookmarkService bookmarks() {
            return bookmarks;
        }

        @Override
        public TextService text() {
            return text;
        }
    }

    static class EngineSourceCatalogService implements SourceCatalogService {
        private IPEDMultiSource multiSource;
        private final Map<Integer, String> sourceIntToString = new HashMap<>();
        private final Map<String, Integer> sourceStringToInt = new HashMap<>();
        private final Map<String, String> sourcePathToStringID = new HashMap<>();

        @Override
        public synchronized void init(String urlToAskSources) throws Exception {
            sourceIntToString.clear();
            sourceStringToInt.clear();
            sourcePathToStringID.clear();

            boolean confInited = false;
            List<IIPEDSource> sources = new ArrayList<>();
            JSONArray arr = askSources(urlToAskSources);
            for (Object object : arr) {
                JSONObject jsonobj = (JSONObject) object;
                String id = (String) jsonobj.get("id");
                File file = new File((String) jsonobj.get("path"));
                sourcePathToStringID.put(file.toString(), id);

                if (!confInited) {
                    Configuration.getInstance().loadConfigurables(file + File.separator + "iped", true);
                    confInited = true;
                }
                sources.add(new IPEDSource(file));
            }

            multiSource = new IPEDMultiSource(sources);
            for (int i = 0; i < multiSource.getAtomicSources().size(); i++) {
                IIPEDSource source = multiSource.getAtomicSourceBySourceId(i);
                String path = source.getCaseDir().toString();
                String id = sourcePathToStringID.get(path);
                if (sourceStringToInt.containsKey(id)) {
                    throw new RuntimeException("duplicated id: " + id);
                }
                sourceStringToInt.put(id, i);
                sourceIntToString.put(i, id);
            }
        }

        @Override
        public synchronized List<SourceDescriptor> listSources() throws Exception {
            List<SourceDescriptor> data = new ArrayList<>();
            for (IIPEDSource source : multiSource.getAtomicSources()) {
                int id = source.getSourceId();
                data.add(new SourceDescriptor(sourceIntToString.get(id), source.getCaseDir().toString()));
            }
            return data;
        }

        @Override
        public synchronized void addSource(SourceDescriptor source) {
            String id = source.getId();
            String path = source.getPath();
            if (sourceStringToInt.containsKey(id)) {
                throw new RuntimeException("duplicated id: " + id);
            }
            sourcePathToStringID.put(path, id);

            List<IPEDSource> sources = multiSource.getAtomicSources();
            int last = sources.size();
            sources.add(new IPEDSource(new File(path)));
            if (last + 1 != sources.size()) {
                throw new RuntimeException("concurrency error adding source");
            }
            multiSource.init();
            IIPEDSource loaded = multiSource.getAtomicSourceBySourceId(last);
            String realpath = loaded.getCaseDir().toString();
            if (!path.equals(realpath)) {
                throw new RuntimeException("error adding source; expected " + path + " got " + realpath);
            }
            sourceStringToInt.put(id, last);
            sourceIntToString.put(last, id);
        }

        @Override
        public synchronized SourceDescriptor getSource(String sourceId) {
            IIPEDSource source = getSourceHandle(sourceId);
            return new SourceDescriptor(sourceId, source.getCaseDir().toString());
        }

        @Override
        public synchronized IIPEDSource getSourceHandle(String sourceId) {
            int id = sourceStringToInt.get(sourceId);
            return multiSource.getAtomicSourceBySourceId(id);
        }

        @Override
        public synchronized String getSourceStringId(int sourceId) {
            return sourceIntToString.get(sourceId);
        }

        @Override
        public synchronized int getSourceIntId(String sourceId) {
            return sourceStringToInt.get(sourceId);
        }

        synchronized IPEDMultiSource getMultiSource() {
            return multiSource;
        }

        private JSONArray askSources(String urlToAskSources) throws Exception {
            InputStream in;
            if ((new File(urlToAskSources)).exists()) {
                in = new FileInputStream(urlToAskSources);
            } else {
                in = (new URL(urlToAskSources)).openConnection().getInputStream();
            }
            try (in) {
                return (JSONArray) JSONValue.parseWithException(new InputStreamReader(in));
            }
        }
    }

    static class EngineSearchService implements SearchService {
        private final EngineSourceCatalogService sources;

        EngineSearchService(EngineSourceCatalogService sources) {
            this.sources = sources;
        }

        @Override
        public List<DocRef> search(String query, String sourceId) throws Exception {
            String escapedQuery = query.replaceAll("/", "\\\\/");
            List<DocRef> docs = new ArrayList<>();
            if (sourceId == null || sourceId.isEmpty()) {
                IPEDSearcher searcher = new IPEDSearcher(sources.getMultiSource(), escapedQuery);
                IMultiSearchResult result = searcher.multiSearch();
                for (IItemId id : result.getIterator()) {
                    docs.add(new DocRef(sources.getSourceStringId(id.getSourceId()), id.getId()));
                }
            } else {
                IPEDSource source = (IPEDSource) sources.getSourceHandle(sourceId);
                IIPEDSearcher searcher = new IPEDSearcher(source, escapedQuery);
                SearchResult result = searcher.search();
                for (int id : result.getIds()) {
                    docs.add(new DocRef(sourceId, id));
                }
            }
            return docs;
        }
    }

    static class EngineSelectionService implements SelectionService {
        private final EngineSourceCatalogService sources;

        EngineSelectionService(EngineSourceCatalogService sources) {
            this.sources = sources;
        }

        @Override
        public List<DocRef> getSelected() throws Exception {
            IIPEDSearcher searcher = new IPEDSearcher(sources.getMultiSource(), "");
            IMultiSearchResult result = searcher.multiSearch();
            result = sources.getMultiSource().getMultiBookmarks().filterChecked(result);
            List<DocRef> docs = new ArrayList<>();
            for (IItemId id : result.getIterator()) {
                docs.add(new DocRef(sources.getSourceStringId(id.getSourceId()), id.getId()));
            }
            return docs;
        }

        @Override
        public void add(List<DocRef> docs) {
            IMultiBookmarks mm = sources.getMultiSource().getMultiBookmarks();
            for (DocRef doc : docs) {
                mm.setChecked(true, new ItemId(sources.getSourceIntId(doc.getSource()), doc.getId()));
            }
            mm.saveState();
        }

        @Override
        public void remove(List<DocRef> docs) {
            IMultiBookmarks mm = sources.getMultiSource().getMultiBookmarks();
            for (DocRef doc : docs) {
                mm.setChecked(false, new ItemId(sources.getSourceIntId(doc.getSource()), doc.getId()));
            }
            mm.saveState();
        }
    }

    static class EngineBookmarkService implements BookmarkService {
        private final EngineSourceCatalogService sources;

        EngineBookmarkService(EngineSourceCatalogService sources) {
            this.sources = sources;
        }

        @Override
        public Set<String> listBookmarks() {
            return new LinkedHashSet<>(sources.getMultiSource().getMultiBookmarks().getBookmarkSet());
        }

        @Override
        public List<DocRef> listBookmarkDocs(String bookmark) throws Exception {
            IPEDSearcher searcher = new IPEDSearcher(sources.getMultiSource(), "");
            IMultiSearchResult result = searcher.multiSearch();
            result = sources.getMultiSource().getMultiBookmarks().filterBookmarks(result, Collections.singleton(bookmark));
            List<DocRef> docs = new ArrayList<>();
            for (IItemId id : result.getIterator()) {
                docs.add(new DocRef(sources.getSourceStringId(id.getSourceId()), id.getId()));
            }
            return docs;
        }

        @Override
        public void add(String bookmark, List<DocRef> docs) {
            IMultiBookmarks mm = sources.getMultiSource().getMultiBookmarks();
            ItemIdSet itemIds = new ItemIdSet();
            for (DocRef doc : docs) {
                itemIds.add(new ItemId(sources.getSourceIntId(doc.getSource()), doc.getId()));
            }
            mm.addBookmark(itemIds, bookmark);
            mm.saveState();
        }

        @Override
        public void remove(String bookmark, List<DocRef> docs) {
            IMultiBookmarks mm = sources.getMultiSource().getMultiBookmarks();
            ItemIdSet itemIds = new ItemIdSet();
            for (DocRef doc : docs) {
                itemIds.add(new ItemId(sources.getSourceIntId(doc.getSource()), doc.getId()));
            }
            mm.removeBookmark(itemIds, bookmark);
            mm.saveState();
        }

        @Override
        public void create(String bookmark) {
            IMultiBookmarks mm = sources.getMultiSource().getMultiBookmarks();
            mm.newBookmark(bookmark);
            mm.saveState();
        }

        @Override
        public void delete(String bookmark) {
            IMultiBookmarks mm = sources.getMultiSource().getMultiBookmarks();
            mm.delBookmark(bookmark);
            mm.saveState();
        }

        @Override
        public void rename(String oldBookmark, String newBookmark) {
            IMultiBookmarks mm = sources.getMultiSource().getMultiBookmarks();
            mm.renameBookmark(oldBookmark, newBookmark);
            mm.saveState();
        }
    }

    static class EngineTextService implements TextService {
        private final EngineSourceCatalogService sources;

        EngineTextService(EngineSourceCatalogService sources) {
            this.sources = sources;
        }

        @Override
        public void writeText(String sourceId, int id, OutputStream output) throws Exception {
            IIPEDSource source = sources.getSourceHandle(sourceId);
            IItem item = source.getItemByID(id);
            StandardParser parser = new StandardParser();
            ParseContext context = getTikaContext(item, parser, (IPEDSource) source);
            Metadata metadata = new Metadata();
            ParsingTaskSupport.fillMetadata(item, metadata);
            parser.setPrintMetadata(false);

            ContentHandler handler = new ToTextContentHandler(output, "UTF-8");
            try (var is = item.getTikaStream()) {
                parser.parse(is, handler, metadata, context);
            }
        }

        private ParseContext getTikaContext(IItem item, StandardParser parser, IPEDSource source) throws Exception {
            return ParsingTaskContextFactory.create(item, parser, ConfigurationManager.get(), source, false);
        }
    }
}
