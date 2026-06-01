package iped.viewers.web;

public enum RenditionKind {
    TEXT("text", "text/plain; charset=UTF-8"),
    HTML("html", "text/html; charset=UTF-8"),
    IMAGE("image", "image/png"),
    PDF("pdf", "application/pdf"),
    BYTES("bytes", "application/octet-stream");

    private final String label;
    private final String mediaType;

    RenditionKind(String label, String mediaType) {
        this.label = label;
        this.mediaType = mediaType;
    }

    public String label() {
        return label;
    }

    public String mediaType() {
        return mediaType;
    }

    public static RenditionKind fromLabel(String label) {
        for (RenditionKind k : values()) {
            if (k.label.equals(label)) return k;
        }
        throw new IllegalArgumentException("Unknown rendition kind: " + label);
    }
}
