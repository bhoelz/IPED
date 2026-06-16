package iped.viewers.api.capability;

import java.util.Objects;
import java.util.Set;

/**
 * Immutable description of a viewer's capabilities, rendering target, and
 * web-portability classification.
 *
 * <p>Returned by {@link IViewerCapability#getCapabilityDescriptor()} so the
 * framework can route items to the right viewer without instantiating one.
 *
 * <h3>Usage</h3>
 * <pre>
 * ViewerCapabilityDescriptor d = new ViewerCapabilityDescriptor.Builder("TextViewer")
 *     .mimeTypes("text/plain", "text/html")
 *     .renderTarget(RenderTarget.WEB_HTML)
 *     .classification(ViewerClassification.WEB_PORTABLE)
 *     .hitsSupported(true)
 *     .searchSupported(true)
 *     .build();
 * </pre>
 */
public final class ViewerCapabilityDescriptor {

    private final String viewerName;
    private final Set<String> supportedMimeTypes;
    private final RenderTarget primaryRenderTarget;
    private final ViewerClassification classification;
    private final String classificationNote;
    private final boolean hitsSupported;
    private final boolean searchSupported;
    private final boolean toolbarSupported;

    private ViewerCapabilityDescriptor(Builder b) {
        this.viewerName          = Objects.requireNonNull(b.viewerName, "viewerName");
        this.supportedMimeTypes  = Set.copyOf(b.supportedMimeTypes);
        this.primaryRenderTarget = Objects.requireNonNull(b.primaryRenderTarget, "primaryRenderTarget");
        this.classification      = Objects.requireNonNull(b.classification, "classification");
        this.classificationNote  = b.classificationNote != null ? b.classificationNote : "";
        this.hitsSupported       = b.hitsSupported;
        this.searchSupported     = b.searchSupported;
        this.toolbarSupported    = b.toolbarSupported;
    }

    public String getViewerName()                    { return viewerName; }
    public Set<String> getSupportedMimeTypes()       { return supportedMimeTypes; }
    public RenderTarget getPrimaryRenderTarget()     { return primaryRenderTarget; }
    public ViewerClassification getClassification()  { return classification; }
    public String getClassificationNote()            { return classificationNote; }
    public boolean isHitsSupported()                 { return hitsSupported; }
    public boolean isSearchSupported()               { return searchSupported; }
    public boolean isToolbarSupported()              { return toolbarSupported; }

    // ── Builder ───────────────────────────────────────────────────────────────

    public static final class Builder {
        private final String viewerName;
        private Set<String> supportedMimeTypes = Set.of();
        private RenderTarget primaryRenderTarget;
        private ViewerClassification classification;
        private String classificationNote;
        private boolean hitsSupported;
        private boolean searchSupported;
        private boolean toolbarSupported;

        public Builder(String viewerName) {
            this.viewerName = viewerName;
        }

        public Builder mimeTypes(String... types)              { this.supportedMimeTypes  = Set.of(types); return this; }
        public Builder mimeTypes(Set<String> types)            { this.supportedMimeTypes  = Set.copyOf(types); return this; }
        public Builder renderTarget(RenderTarget t)            { this.primaryRenderTarget = t; return this; }
        public Builder classification(ViewerClassification c)  { this.classification      = c; return this; }
        public Builder classificationNote(String n)            { this.classificationNote  = n; return this; }
        public Builder hitsSupported(boolean v)                { this.hitsSupported       = v; return this; }
        public Builder searchSupported(boolean v)              { this.searchSupported     = v; return this; }
        public Builder toolbarSupported(boolean v)             { this.toolbarSupported    = v; return this; }

        public ViewerCapabilityDescriptor build() {
            return new ViewerCapabilityDescriptor(this);
        }
    }

    @Override
    public String toString() {
        return "ViewerCapabilityDescriptor{name=" + viewerName
                + ", target=" + primaryRenderTarget
                + ", class=" + classification + "}";
    }
}
