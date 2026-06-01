package iped.viewers.web;

public class UnsupportedRenditionException extends RuntimeException {

    public UnsupportedRenditionException(String mimeType, RenditionKind kind) {
        super("No renderer supports " + kind.label() + " rendition for mime type: " + mimeType);
    }
}
