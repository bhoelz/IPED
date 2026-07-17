package iped.engine.webapi;

import iped.data.IIPEDSource;
import iped.data.IItem;
import iped.data.IItemId;
import iped.data.IMultiBookmarks;
import iped.engine.config.Configuration;
import iped.engine.config.ConfigurationManager;
import iped.engine.config.EngineConfigContributor;
import iped.engine.data.IPEDMultiSource;
import iped.engine.data.IPEDSource;
import iped.engine.data.ItemId;
import iped.engine.data.ItemIdSet;
import iped.engine.search.IPEDSearcher;
import iped.engine.task.ParsingTaskContextFactory;
import iped.engine.task.ParsingTaskSupport;
import iped.engine.webapi.spi.*;
import iped.parsers.standard.StandardParser;
import iped.search.IIPEDSearcher;
import iped.search.IMultiSearchResult;
import iped.search.SearchResult;
import iped.viewers.web.*;
import java.io.*;
import java.net.URL;
import java.util.*;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.parser.ParseContext;
import org.apache.tika.sax.ToTextContentHandler;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;
import org.xml.sax.ContentHandler;

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
    private final RenditionService renditions = new EngineRenditionService(sources);
    private final ViewerSessionService viewerSessions =
        new EngineViewerSessionService(sources, renditions);

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

    @Override
    public RenditionService renditions() {
      return renditions;
    }

    @Override
    public ViewerSessionService viewerSessions() {
      return viewerSessions;
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

      // urlToAskSources is optional — null or empty means start with no open cases.
      JSONArray arr =
          (urlToAskSources != null && !urlToAskSources.isBlank())
              ? askSources(urlToAskSources)
              : new JSONArray();

      for (Object object : arr) {
        JSONObject jsonobj = (JSONObject) object;
        String id = (String) jsonobj.get("id");
        File file = new File((String) jsonobj.get("path"));
        if (!file.exists()) {
          throw new IllegalArgumentException(
              "Source path does not exist: "
                  + file.getAbsolutePath()
                  + " (id="
                  + id
                  + "). Check your --sources file.");
        }
        sourcePathToStringID.put(file.toString(), id);

        if (!confInited) {
          Configuration.getInstance()
              .loadConfigurables(
                  file + File.separator + "iped", true, EngineConfigContributor.INSTANCE);
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
    public synchronized void removeSource(String sourceId) throws Exception {
      Integer intId = sourceStringToInt.get(sourceId);
      if (intId == null) {
        throw new IllegalArgumentException("Unknown source: " + sourceId);
      }
      IIPEDSource handle = multiSource.getAtomicSourceBySourceId(intId);
      // Remove from all tracking maps so future lookups return null/404
      sourceStringToInt.remove(sourceId);
      sourceIntToString.remove(intId);
      if (handle != null) {
        String path = handle.getCaseDir().toString();
        sourcePathToStringID.remove(path);
        try {
          handle.close();
        } catch (Exception ignored) {
        }
      }
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

    @Override
    public SearchPage searchPaginated(String query, String sourceId, int offset, int limit)
        throws Exception {
      List<DocRef> all = search(query, sourceId);
      long total = all.size();
      int from = Math.min(offset, (int) total);
      int to = Math.min(from + limit, (int) total);
      List<DocRef> page = all.subList(from, to);

      List<SearchResultItem> items = new ArrayList<>(page.size());
      for (DocRef ref : page) {
        IIPEDSource src = sources.getSourceHandle(ref.getSource());
        IItem item = ((IPEDSource) src).getItemByID(ref.getId());
        String mediaTypeStr = item.getMediaTypeString();
        items.add(
            new SearchResultItem(
                ref.getSource(),
                ref.getId(),
                item.getName(),
                item.getPath(),
                mediaTypeStr,
                item.getLength(),
                item.getHash(),
                item.getModDate(),
                item.getCreationDate(),
                item.isDeleted(),
                item.isDir(),
                item.getCategorySet()));
      }
      return new SearchPage(items, total, offset, limit);
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
      result =
          sources
              .getMultiSource()
              .getMultiBookmarks()
              .filterBookmarks(result, Collections.singleton(bookmark));
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

    private ParseContext getTikaContext(IItem item, StandardParser parser, IPEDSource source)
        throws Exception {
      return ParsingTaskContextFactory.create(
          item, parser, ConfigurationManager.get(), source, false);
    }
  }

  static class EngineRenditionService implements RenditionService {
    private static final WebRendererRegistry REGISTRY = WebRendererRegistry.defaultRegistry();

    private final EngineSourceCatalogService sources;

    EngineRenditionService(EngineSourceCatalogService sources) {
      this.sources = sources;
    }

    @Override
    public List<RenditionDescriptor> available(
        String sourceId, int itemId, String mimeType, String baseUrl) {
      List<RenditionDescriptor> result = new ArrayList<>();
      String prefix = baseUrl + "/sources/" + sourceId + "/docs/" + itemId;
      for (RenditionKind kind : REGISTRY.availableRenditions(mimeType)) {
        String url =
            switch (kind) {
              case BYTES -> prefix + "/content";
              default -> prefix + "/renditions/" + kind.label();
            };
        result.add(new RenditionDescriptor(kind.label(), url));
      }
      // Always include bytes (raw content endpoint)
      boolean hasBytes = result.stream().anyMatch(r -> "bytes".equals(r.getKind()));
      if (!hasBytes) {
        result.add(new RenditionDescriptor("bytes", prefix + "/content"));
      }
      return result;
    }

    @Override
    public Map<String, Object> capabilities(String sourceId, int itemId, String mimeType) {
      WebRenderer renderer = REGISTRY.findAny(mimeType);
      Map<String, Object> caps = new HashMap<>();
      if (renderer == null) {
        caps.put("viewerId", "unsupported");
        caps.put("search", false);
        caps.put("hitsMode", "none");
        caps.put("toolbarSupported", false);
        caps.put("toolbarVisibleByDefault", false);
      } else {
        ViewerCapabilities vc = renderer.capabilities(mimeType);
        caps.put("viewerId", renderer.id());
        caps.put("search", vc.isSearch());
        caps.put("hitsMode", vc.getHitsMode().label());
        caps.put("toolbarSupported", vc.isToolbarSupported());
        caps.put("toolbarVisibleByDefault", vc.isToolbarVisibleByDefault());
      }
      return caps;
    }

    @Override
    public void writeRendition(
        String sourceId, int itemId, String kind, int page, int width, OutputStream out)
        throws Exception {
      IIPEDSource source = sources.getSourceHandle(sourceId);
      IItem item = source.getItemByID(itemId);
      String mimeType = item.getMediaType().toString();
      RenditionKind renditionKind = RenditionKind.fromLabel(kind);
      WebRenderer renderer = REGISTRY.find(mimeType, renditionKind);
      if (renderer == null) {
        throw new IllegalArgumentException(
            "No renderer for kind=" + kind + " mimeType=" + mimeType);
      }
      RenderRequest req =
          RenderRequest.builder(item, mimeType, renditionKind).page(page).widthHint(width).build();
      renderer.render(req, out);
    }
  }

  static class EngineViewerSessionService implements ViewerSessionService {
    private final EngineSourceCatalogService sources;
    private final RenditionService renditions;

    EngineViewerSessionService(EngineSourceCatalogService sources, RenditionService renditions) {
      this.sources = sources;
      this.renditions = renditions;
    }

    @Override
    public Map<String, Object> openSession(
        String sourceId,
        int itemId,
        String mimeType,
        List<String> highlightTerms,
        String preferredViewerId,
        String baseUrl) {
      String sessionId = java.util.UUID.randomUUID().toString();
      Map<String, Object> caps = renditions.capabilities(sourceId, itemId, mimeType);
      List<RenditionDescriptor> renditionList =
          renditions.available(sourceId, itemId, mimeType, baseUrl);

      Map<String, Object> response = new HashMap<>();
      response.put("viewerSessionId", sessionId);
      response.put("viewerId", caps.get("viewerId"));
      response.put("capabilities", caps);
      response.put("renditions", renditionList);
      return response;
    }

    @Override
    public Map<String, Object> search(String sessionId, String term, boolean caseSensitive) {
      // Client-side search for text/html renditions; server returns empty hit state.
      // PDF/image search is not implemented in MVP.
      return Map.of("totalHits", 0, "currentHit", 0, "hitRanges", List.of());
    }

    @Override
    public Map<String, Object> navigateHit(String sessionId, String direction, boolean wrap) {
      return Map.of("totalHits", 0, "currentHit", 0);
    }
  }
}
