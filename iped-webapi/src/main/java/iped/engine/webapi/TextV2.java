package iped.engine.webapi;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.*;

/**
 * v2 item text endpoint.
 *
 * <p>Mirrors the v1 {@link Text} endpoint at the v2 URL shape
 * ({@code /v2/sources/{sourceId}/items/{id}/text}) and adds:
 * <ul>
 *   <li>{@code ?highlight=term1+term2} — caller-supplied highlight terms are
 *       wrapped in {@code <mark>} tags (plain-text callers receive raw text,
 *       callers requesting {@code text/html} receive the marked-up version).</li>
 *   <li>{@code ?limit=N} — truncates output to the first N bytes (useful for
 *       preview snippets).</li>
 * </ul>
 *
 * <p>When the source or item is not found, returns {@code 404}
 * (the v1 endpoint throws an unhandled exception).
 */
@Api(value = "Items v2")
@Path("v2/sources/{sourceId}/items/{id}/text")
public class TextV2 {

    @ApiOperation("Stream item text content (UTF-8)")
    @GET
    @Produces({MediaType.TEXT_PLAIN + "; charset=UTF-8", MediaType.TEXT_HTML + "; charset=UTF-8"})
    public Response text(
            @PathParam("sourceId") String sourceId,
            @PathParam("id") int id,
            @QueryParam("highlight") String highlight,
            @QueryParam("limit") @DefaultValue("-1") long limit,
            @Context HttpHeaders headers) {

        var source = Sources.getSource(sourceId);
        if (source == null) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity("Source not found: " + sourceId).build();
        }

        var item = source.getItemByID(id);
        if (item == null) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity("Item not found: " + id).build();
        }

        boolean wantHtml = headers.getAcceptableMediaTypes().stream()
                .anyMatch(m -> m.isCompatible(MediaType.TEXT_HTML_TYPE));

        String[] terms = highlight != null && !highlight.isBlank()
                ? highlight.trim().split("\\s+") : new String[0];

        jakarta.ws.rs.core.StreamingOutput stream = out -> {
            try {
                var writer = new java.io.OutputStreamWriter(
                        limit > 0 ? new LimitOutputStream(out, limit) : out,
                        java.nio.charset.StandardCharsets.UTF_8);

                // Delegate text extraction to the v1 SPI
                var buf = new java.io.ByteArrayOutputStream();
                Sources.services().text().writeText(sourceId, id, buf);
                String text = buf.toString(java.nio.charset.StandardCharsets.UTF_8);

                if (wantHtml && terms.length > 0) {
                    text = escapeHtml(text);
                    for (String term : terms) {
                        text = text.replaceAll("(?i)" + java.util.regex.Pattern.quote(term),
                                "<mark>$0</mark>");
                    }
                    writer.write("<pre>");
                    writer.write(text);
                    writer.write("</pre>");
                } else {
                    writer.write(text);
                }
                writer.flush();
            } catch (Exception e) {
                throw new WebApplicationException(e);
            }
        };

        return Response.ok(stream)
                .type(wantHtml && terms.length > 0
                        ? MediaType.TEXT_HTML + "; charset=UTF-8"
                        : MediaType.TEXT_PLAIN + "; charset=UTF-8")
                .build();
    }

    private static String escapeHtml(String s) {
        return s.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;");
    }

    /** Wraps an OutputStream to enforce a byte limit. */
    private static final class LimitOutputStream extends java.io.FilterOutputStream {
        private long remaining;

        LimitOutputStream(java.io.OutputStream out, long limit) {
            super(out);
            this.remaining = limit;
        }

        @Override
        public void write(int b) throws java.io.IOException {
            if (remaining-- <= 0) throw new LimitReachedException();
            out.write(b);
        }

        @Override
        public void write(byte[] b, int off, int len) throws java.io.IOException {
            if (remaining <= 0) throw new LimitReachedException();
            int n = (int) Math.min(len, remaining);
            out.write(b, off, n);
            remaining -= n;
        }
    }

    private static final class LimitReachedException extends java.io.IOException {
        LimitReachedException() { super("limit"); }
    }
}
