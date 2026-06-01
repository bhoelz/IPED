package iped.engine.webapi.spi;

import java.util.List;
import java.util.Map;

public interface ViewerSessionService {

    /**
     * Opens a viewer session for the given item and returns session metadata.
     *
     * Map keys: "viewerSessionId", "viewerId", plus nested maps for
     * "capabilities" and a List of RenditionDescriptors under "renditions".
     */
    Map<String, Object> openSession(String sourceId, int itemId, String mimeType,
            List<String> highlightTerms, String preferredViewerId, String baseUrl);

    /**
     * Performs an in-viewer text search within the session.
     *
     * Map keys: "totalHits", "currentHit", "hitRanges" (List of Map with "start","end","page").
     */
    Map<String, Object> search(String sessionId, String term, boolean caseSensitive);

    /**
     * Navigates to the next or previous hit.
     *
     * Map keys: "totalHits", "currentHit".
     */
    Map<String, Object> navigateHit(String sessionId, String direction, boolean wrap);
}
