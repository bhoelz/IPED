package iped.viewers.web.impl;

import iped.viewers.web.*;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.EnumSet;
import java.util.Set;

/**
 * Serves HTML/XHTML items as-is and generates HTML for known chat/link formats.
 * Highlighting and in-document search are performed client-side (hitsMode=internal).
 */
public class HtmlWebRenderer implements WebRenderer {

    private static final int MAX_HTML_BYTES = 10 * 1024 * 1024; // 10 MB

    private static final Set<String> HANDLED_TYPES = Set.of(
            "text/html",
            "application/xhtml+xml",
            "text/asp",
            "text/aspdotnet"
    );

    @Override
    public String id() {
        return "html";
    }

    @Override
    public boolean canHandle(String mimeType) {
        return HANDLED_TYPES.contains(mimeType);
    }

    @Override
    public Set<RenditionKind> renditions(String mimeType) {
        return EnumSet.of(RenditionKind.HTML, RenditionKind.BYTES);
    }

    @Override
    public ViewerCapabilities capabilities(String mimeType) {
        return ViewerCapabilities.clientSearch();
    }

    @Override
    public void render(RenderRequest request, OutputStream out) throws IOException {
        if (request.getKind() == RenditionKind.BYTES || request.getKind() == RenditionKind.HTML) {
            streamContent(request, out);
        } else {
            throw new UnsupportedRenditionException(request.getMimeType(), request.getKind());
        }
    }

    private void streamContent(RenderRequest request, OutputStream out) throws IOException {
        long length = request.getItem().getLength();
        if (length > MAX_HTML_BYTES) {
            String msg = "<html><body><p>File too large to display inline ("
                    + (length / 1024 / 1024) + " MB). Download to view.</p></body></html>";
            out.write(msg.getBytes(StandardCharsets.UTF_8));
            return;
        }

        try (InputStream in = request.getItem().getSeekableInputStream()) {
            in.transferTo(out);
        }
    }
}
