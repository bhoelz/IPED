package iped.viewers.web.impl;

import iped.viewers.web.*;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.ImageType;
import org.apache.pdfbox.rendering.PDFRenderer;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.EnumSet;
import java.util.Set;

/**
 * Serves PDFs as raw bytes (browser renders natively) and optionally renders
 * individual pages to PNG using PDFBox for thumbnail/search support.
 */
@Slf4j
public class PdfWebRenderer implements WebRenderer {

    private static final float DEFAULT_DPI = 150f;

    @Override
    public String id() {
        return "pdf";
    }

    @Override
    public boolean canHandle(String mimeType) {
        return "application/pdf".equals(mimeType);
    }

    @Override
    public Set<RenditionKind> renditions(String mimeType) {
        return EnumSet.of(RenditionKind.PDF, RenditionKind.IMAGE, RenditionKind.BYTES);
    }

    @Override
    public ViewerCapabilities capabilities(String mimeType) {
        // Browser PDF viewer handles search; no server-side hit navigation needed
        return ViewerCapabilities.noSearch();
    }

    @Override
    public void render(RenderRequest request, OutputStream out) throws IOException {
        switch (request.getKind()) {
            case PDF, BYTES -> streamPdfBytes(request, out);
            case IMAGE -> renderPageAsImage(request, out);
            default -> throw new UnsupportedRenditionException(request.getMimeType(), request.getKind());
        }
    }

    private void streamPdfBytes(RenderRequest request, OutputStream out) throws IOException {
        try (InputStream in = request.getItem().getSeekableInputStream()) {
            in.transferTo(out);
        }
    }

    private void renderPageAsImage(RenderRequest request, OutputStream out) throws IOException {
        int targetPage = Math.max(0, request.getPage());
        byte[] pdfBytes;
        try (InputStream in = request.getItem().getSeekableInputStream()) {
            pdfBytes = in.readAllBytes();
        }

        try (PDDocument doc = Loader.loadPDF(pdfBytes)) {
            int numPages = doc.getNumberOfPages();
            int page = Math.min(targetPage, numPages - 1);
            PDFRenderer renderer = new PDFRenderer(doc);
            renderer.setSubsamplingAllowed(true);
            BufferedImage image = renderer.renderImageWithDPI(page, DEFAULT_DPI, ImageType.RGB);
            ImageIO.write(image, "png", out);
        } catch (Exception e) {
            log.warn("Failed to render PDF page {} for item {}: {}", targetPage,
                    request.getItem().getName(), e.getMessage());
            throw new IOException("PDF page rendering failed", e);
        }
    }
}
