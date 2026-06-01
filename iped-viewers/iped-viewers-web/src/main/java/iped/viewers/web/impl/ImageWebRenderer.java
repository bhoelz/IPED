package iped.viewers.web.impl;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.EnumSet;
import java.util.Iterator;
import java.util.Set;

import javax.imageio.ImageIO;
import javax.imageio.ImageReadParam;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import iped.viewers.web.RenderRequest;
import iped.viewers.web.RenditionKind;
import iped.viewers.web.UnsupportedRenditionException;
import iped.viewers.web.ViewerCapabilities;
import iped.viewers.web.WebRenderer;

/**
 * Renders image items (including multi-page TIFF) as PNG using headless ImageIO.
 * TwelveMonkeys plugins on the classpath extend format support transparently.
 */
public class ImageWebRenderer implements WebRenderer {

    private static final Logger LOGGER = LoggerFactory.getLogger(ImageWebRenderer.class);

    @Override
    public String id() {
        return "image";
    }

    @Override
    public boolean canHandle(String mimeType) {
        if (mimeType == null) return false;
        return mimeType.startsWith("image/");
    }

    @Override
    public Set<RenditionKind> renditions(String mimeType) {
        return EnumSet.of(RenditionKind.IMAGE, RenditionKind.BYTES);
    }

    @Override
    public ViewerCapabilities capabilities(String mimeType) {
        return ViewerCapabilities.noSearch();
    }

    @Override
    public void render(RenderRequest request, OutputStream out) throws IOException {
        switch (request.getKind()) {
            case IMAGE -> renderImage(request, out);
            case BYTES -> streamBytes(request, out);
            default -> throw new UnsupportedRenditionException(request.getMimeType(), request.getKind());
        }
    }

    private void renderImage(RenderRequest request, OutputStream out) throws IOException {
        int targetPage = Math.max(0, request.getPage());

        try (InputStream in = request.getItem().getSeekableInputStream();
             ImageInputStream iis = ImageIO.createImageInputStream(in)) {

            Iterator<ImageReader> readers = ImageIO.getImageReaders(iis);
            if (!readers.hasNext()) {
                // Fallback: pass raw bytes; browser may be able to display natively
                streamBytes(request, out);
                return;
            }

            ImageReader reader = readers.next();
            try {
                reader.setInput(iis, false, true);
                int numImages = reader.getNumImages(true);
                int page = Math.min(targetPage, numImages - 1);

                ImageReadParam param = reader.getDefaultReadParam();
                BufferedImage image = reader.read(page, param);
                ImageIO.write(image, "png", out);
            } finally {
                reader.dispose();
            }
        } catch (Exception e) {
            LOGGER.warn("Failed to decode image for item {}: {}", request.getItem().getName(), e.getMessage());
            throw new IOException("Image decoding failed", e);
        }
    }

    private void streamBytes(RenderRequest request, OutputStream out) throws IOException {
        try (InputStream in = request.getItem().getSeekableInputStream()) {
            in.transferTo(out);
        }
    }
}
