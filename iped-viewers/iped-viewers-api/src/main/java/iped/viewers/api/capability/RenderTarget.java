package iped.viewers.api.capability;

/**
 * Describes where and how a viewer renders its content.
 *
 * <p>Used in {@link ViewerCapabilityDescriptor} to classify each viewer's primary
 * rendering path and guide framework decisions about deployment topology:
 * browser-side, Swing desktop, or companion-app bridge.
 */
public enum RenderTarget {

    /**
     * Content is fetched from the server and rendered entirely in the browser.
     * The viewer is a web component (Angular island or raw custom element) that
     * drives {@code iped-webapi} endpoints for data.
     *
     * <p>Examples: text viewer, image viewer, PDF via PDF.js, hex viewer island.
     */
    WEB_HTML,

    /**
     * Content is rendered in a Swing component within the IPED desktop app (iped-app).
     * Not accessible from the browser UI.
     *
     * <p>Examples: legacy MetadataViewer, CAD viewer, audio player (JavaFX).
     */
    NATIVE_SWING,

    /**
     * Content is rendered in the companion desktop app via the bridge protocol
     * (workstream 2 of the root roadmap). The browser delegates to the native app
     * through a lifecycle handoff.
     *
     * <p>Examples: LibreOffice embedding, ReferencedFileViewer.
     */
    NATIVE_BRIDGE
}
