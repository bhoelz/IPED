package iped.engine.webapi.spi;

public interface WebApiServices {
    SourceCatalogService sources();

    SearchService search();

    SelectionService selection();

    BookmarkService bookmarks();

    TextService text();

    RenditionService renditions();

    ViewerSessionService viewerSessions();
}
