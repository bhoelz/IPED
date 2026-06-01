package iped.viewers.web;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

public class WebRendererRegistry {

    private final List<WebRenderer> renderers;

    public WebRendererRegistry(List<WebRenderer> renderers) {
        this.renderers = Collections.unmodifiableList(new ArrayList<>(renderers));
    }

    public static WebRendererRegistry defaultRegistry() {
        List<WebRenderer> defaults = new ArrayList<>();
        defaults.add(new iped.viewers.web.impl.HtmlWebRenderer());
        defaults.add(new iped.viewers.web.impl.ImageWebRenderer());
        defaults.add(new iped.viewers.web.impl.PdfWebRenderer());
        defaults.add(new iped.viewers.web.impl.TextWebRenderer());
        return new WebRendererRegistry(defaults);
    }

    /** Returns the first renderer that canHandle the mimeType and has the requested kind. */
    public WebRenderer find(String mimeType, RenditionKind kind) {
        for (WebRenderer r : renderers) {
            if (r.canHandle(mimeType) && r.renditions(mimeType).contains(kind)) {
                return r;
            }
        }
        return null;
    }

    /** Returns all rendition kinds available across all renderers for this mimeType. */
    public Set<RenditionKind> availableRenditions(String mimeType) {
        java.util.EnumSet<RenditionKind> result = java.util.EnumSet.noneOf(RenditionKind.class);
        for (WebRenderer r : renderers) {
            if (r.canHandle(mimeType)) {
                result.addAll(r.renditions(mimeType));
            }
        }
        return Collections.unmodifiableSet(result);
    }

    /** Returns the first renderer that canHandle the mimeType, or null. */
    public WebRenderer findAny(String mimeType) {
        for (WebRenderer r : renderers) {
            if (r.canHandle(mimeType)) return r;
        }
        return null;
    }
}
