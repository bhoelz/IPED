package iped.viewers.web.impl;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.EnumSet;
import java.util.Set;

import iped.viewers.web.RenderRequest;
import iped.viewers.web.RenditionKind;
import iped.viewers.web.UnsupportedRenditionException;
import iped.viewers.web.ViewerCapabilities;
import iped.viewers.web.WebRenderer;

/**
 * Serves plain-text items as UTF-8 text. Highlighting is done client-side.
 * This renderer acts as a last-resort fallback for any text/* MIME type.
 */
public class TextWebRenderer implements WebRenderer {

    @Override
    public String id() {
        return "text";
    }

    @Override
    public boolean canHandle(String mimeType) {
        if (mimeType == null) return false;
        // Explicit text types; excludes text/html (handled by HtmlWebRenderer)
        return mimeType.startsWith("text/") && !mimeType.startsWith("text/html")
                && !mimeType.equals("text/asp") && !mimeType.equals("text/aspdotnet")
                && !mimeType.equals("text/aspdotnet");
    }

    @Override
    public Set<RenditionKind> renditions(String mimeType) {
        return EnumSet.of(RenditionKind.TEXT, RenditionKind.BYTES);
    }

    @Override
    public ViewerCapabilities capabilities(String mimeType) {
        return ViewerCapabilities.clientSearch();
    }

    @Override
    public void render(RenderRequest request, OutputStream out) throws IOException {
        if (request.getKind() == RenditionKind.TEXT || request.getKind() == RenditionKind.BYTES) {
            try (InputStream in = request.getItem().getSeekableInputStream()) {
                in.transferTo(out);
            }
        } else {
            throw new UnsupportedRenditionException(request.getMimeType(), request.getKind());
        }
    }
}
