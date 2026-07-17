package iped.app.companion;

/**
 * Payload sent to the companion app's {@code POST /open} endpoint to request that a specific
 * evidence item be displayed in a native viewer.
 *
 * @param sourceId case/source identifier (matches iped-webapi source ID)
 * @param docId Lucene document ID within the source
 * @param mimeType MIME type of the item (drives native viewer selection)
 * @param itemName human-readable item name shown in the companion window title
 * @param contentUrl absolute URL of the item's raw content in iped-webapi
 * @param apiKey iped-webapi API key forwarded so the companion can fetch content
 */
public record CompanionHandoffRequest(
    String sourceId,
    int docId,
    String mimeType,
    String itemName,
    String contentUrl,
    String apiKey) {}
