package iped.engine.mcp;

/**
 * Wraps evidence-sourced text in a clearly delimited block to prevent
 * prompt-injection attacks.
 *
 * <p>A forensic case may contain hostile documents that include text crafted
 * to hijack an LLM's instructions. By wrapping all evidence content in
 * {@code <iped-evidence>} XML-like delimiters the system prompt can instruct
 * the model to treat everything inside as opaque data, not as instructions.
 *
 * <h3>Wrapping contract</h3>
 * <pre>
 * &lt;iped-evidence source="{source}" item="{itemId}" media-type="{mediaType}"&gt;
 * {content}
 * &lt;/iped-evidence&gt;
 * </pre>
 * Content longer than {@value #MAX_CHARS} characters is truncated at a word
 * boundary and a {@code [TRUNCATED]} marker is appended before the closing tag.
 *
 * <h3>Binary / non-text content</h3>
 * Binary items (identified by the {@code binary} flag on the caller side) are
 * represented by a single-line placeholder: the LLM cannot usefully read hex
 * dumps, and sending them wastes context. Use {@link #binaryPlaceholder} instead.
 */
public final class EvidenceGuard {

    /** Maximum evidence characters to include in a single MCP response. */
    public static final int MAX_CHARS = 50_000;

    private EvidenceGuard() {}

    /**
     * Wraps {@code rawText} in evidence delimiters, truncating at {@link #MAX_CHARS}.
     *
     * @param source    source ID (e.g. {@code "src0"})
     * @param itemId    composite item ID ({@code "src0:42"} or just the docId)
     * @param mediaType MIME type of the original item
     * @param rawText   extracted text content from iped-webapi
     * @return delimited, possibly truncated evidence block
     */
    public static String wrap(String source, String itemId, String mediaType, String rawText) {
        String safe = rawText == null ? "" : rawText;
        boolean truncated = safe.length() > MAX_CHARS;
        String content = truncated
                ? safe.substring(0, MAX_CHARS) + "\n[TRUNCATED — " + safe.length() + " chars total]"
                : safe;
        return "<iped-evidence"
                + " source=\"" + escapeAttr(source) + "\""
                + " item=\""   + escapeAttr(itemId)  + "\""
                + " media-type=\"" + escapeAttr(mediaType) + "\""
                + (truncated ? " truncated=\"true\"" : "")
                + ">\n"
                + content
                + "\n</iped-evidence>";
    }

    /**
     * Returns a non-text placeholder for binary items.
     *
     * @param source    source ID
     * @param itemId    composite item ID
     * @param mediaType MIME type
     * @param sizeBytes reported size in bytes, or {@code -1} if unknown
     */
    public static String binaryPlaceholder(String source, String itemId,
                                           String mediaType, long sizeBytes) {
        String sizeStr = sizeBytes >= 0 ? sizeBytes + " bytes" : "unknown size";
        return "<iped-evidence"
                + " source=\"" + escapeAttr(source) + "\""
                + " item=\""   + escapeAttr(itemId)  + "\""
                + " media-type=\"" + escapeAttr(mediaType) + "\""
                + " binary=\"true\""
                + ">"
                + "[Binary content — " + mediaType + ", " + sizeStr
                + ". Use iped_get_item_content to download.]"
                + "</iped-evidence>";
    }

    private static String escapeAttr(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;").replace("\"", "&quot;");
    }
}
