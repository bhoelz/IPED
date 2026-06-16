package iped.engine.webapi;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import iped.data.IIPEDSource;
import iped.data.IItem;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.*;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * v2 item content endpoint with HTTP Range request support.
 *
 * <p>The v1 {@link Content} endpoint serves the full item stream without
 * {@code Content-Range} or partial-content support. This endpoint adds:
 * <ul>
 *   <li>RFC 7233 byte-range handling (single range only; multi-range not supported).</li>
 *   <li>A {@code Content-Type} header derived from the item's media type.</li>
 *   <li>An {@code Accept-Ranges: bytes} header so browsers can seek.</li>
 *   <li>v2 URL shape: {@code /v2/sources/{sourceId}/items/{id}/content}</li>
 *   <li>Optional {@code ?format=png} query parameter for on-the-fly image conversion
 *       (e.g. TIFF → PNG so the browser can render it natively).</li>
 *   <li>Optional {@code ?transcode=webm} for server-side audio/video transcoding via
 *       ffmpeg (covers AMR, WMA, 3GPP, and other non-browser-native codecs). Requires
 *       {@code ffmpeg} on {@code PATH}; returns 501 if unavailable.</li>
 * </ul>
 *
 * <p>The hex-viewer island relies on this endpoint for paged binary navigation.
 * The viewer island uses {@code ?format=png} for {@code image/tiff} items.
 */
@Api(value = "Items v2")
@Path("v2/sources/{sourceId}/items/{id}/content")
public class ContentV2 {

    @ApiOperation("Stream item content (supports byte-range requests and on-the-fly format conversion)")
    @GET
    public Response content(
            @PathParam("sourceId") String sourceId,
            @PathParam("id") int id,
            @HeaderParam("Range") String rangeHeader,
            @QueryParam("format") String format,
            @QueryParam("transcode") String transcode) throws IOException {

        IIPEDSource source = Sources.getSource(sourceId);
        if (source == null) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity("{\"error\":\"Source not found: " + sourceId + "\"}").build();
        }

        IItem item = source.getItemByID(id);
        if (item == null) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity("{\"error\":\"Item not found: " + id + "\"}").build();
        }

        // On-the-fly image format conversion (e.g. TIFF → PNG for browser display).
        if ("png".equalsIgnoreCase(format)) {
            return convertToPng(item);
        }

        // Server-side transcode for non-browser-native audio/video codecs.
        if ("webm".equalsIgnoreCase(transcode)) {
            return transcodeToWebm(item);
        }

        long totalLength = item.getLength();
        String mediaType = item.getMediaType() != null
                ? item.getMediaType().toString() : MediaType.APPLICATION_OCTET_STREAM;

        // No range — return the full stream.
        if (rangeHeader == null || rangeHeader.isBlank()) {
            return Response.ok()
                    .header("Accept-Ranges", "bytes")
                    .header("Content-Length", totalLength)
                    .header("Content-Type", mediaType)
                    .header("Content-Disposition",
                            "inline; filename=\"" + item.getName() + "\"")
                    .entity((jakarta.ws.rs.core.StreamingOutput) out -> {
                        try (InputStream is = item.getBufferedInputStream()) {
                            if (is != null) is.transferTo(out);
                        }
                    }).build();
        }

        // Parse "bytes=start-end"
        long[] range = parseRange(rangeHeader, totalLength);
        if (range == null) {
            return Response.status(416) // Range Not Satisfiable
                    .header("Content-Range", "bytes */" + totalLength).build();
        }

        long start = range[0], end = range[1];
        long chunkLen = end - start + 1;

        return Response.status(206) // Partial Content
                .header("Accept-Ranges", "bytes")
                .header("Content-Range", "bytes " + start + "-" + end + "/" + totalLength)
                .header("Content-Length", chunkLen)
                .header("Content-Type", mediaType)
                .entity((jakarta.ws.rs.core.StreamingOutput) out -> {
                    try (InputStream is = item.getBufferedInputStream()) {
                        if (is == null) return;
                        long skipped = is.skip(start);
                        if (skipped < start) return;
                        byte[] buf = new byte[8192];
                        long remaining = chunkLen;
                        int read;
                        while (remaining > 0
                                && (read = is.read(buf, 0, (int) Math.min(buf.length, remaining))) != -1) {
                            out.write(buf, 0, read);
                            remaining -= read;
                        }
                    }
                }).build();
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    /**
     * Converts the item's content to PNG using {@link ImageIO}.
     * Works for any format that the JDK's built-in image readers support
     * (TIFF via {@code com.sun.imageio.plugins.tiff} on JDK 9+, BMP, GIF, JPEG, PNG).
     * Returns 422 if the content cannot be decoded as an image.
     */
    private static Response convertToPng(IItem item) {
        try (InputStream is = item.getBufferedInputStream()) {
            if (is == null) {
                return Response.status(Response.Status.NOT_FOUND)
                        .entity("{\"error\":\"Item has no content stream\"}").build();
            }
            BufferedImage img = ImageIO.read(is);
            if (img == null) {
                return Response.status(422)
                        .entity("{\"error\":\"Cannot decode item as an image\"}").build();
            }
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            ImageIO.write(img, "png", bos);
            byte[] bytes = bos.toByteArray();
            return Response.ok(bytes, "image/png")
                    .header("Content-Length", bytes.length)
                    .header("Content-Disposition", "inline; filename=\"" + item.getName() + ".png\"")
                    .build();
        } catch (IOException e) {
            return Response.serverError()
                    .entity("{\"error\":\"Image conversion failed: " + e.getMessage() + "\"}").build();
        }
    }

    /**
     * Transcodes item content to WebM via {@code ffmpeg} on the server {@code PATH}.
     *
     * <p>Audio-only items are transcoded to {@code audio/webm} (Opus codec, no video
     * stream). Video items are transcoded to {@code video/webm} (VP9 + Opus).
     * Returns 501 when ffmpeg is not found; 500 on transcoding errors.
     */
    private static Response transcodeToWebm(IItem item) {
        String rawMime = item.getMediaType() != null ? item.getMediaType().toString() : "";
        boolean isVideo = rawMime.startsWith("video/");

        List<String> cmd = new ArrayList<>();
        cmd.add("ffmpeg");
        cmd.add("-hide_banner");
        cmd.add("-i"); cmd.add("pipe:0");
        cmd.add("-c:a"); cmd.add("libopus");
        if (isVideo) {
            cmd.add("-c:v"); cmd.add("libvpx-vp9");
            cmd.add("-b:v"); cmd.add("0");       // constant quality mode
            cmd.add("-crf"); cmd.add("30");
        } else {
            cmd.add("-vn");                       // discard any embedded video track
        }
        cmd.add("-f"); cmd.add("webm");
        cmd.add("pipe:1");

        Process proc;
        try {
            proc = new ProcessBuilder(cmd).start();
        } catch (IOException e) {
            return Response.status(501)
                    .entity("{\"error\":\"ffmpeg not available for server-side transcoding\"}")
                    .build();
        }

        // Feed item bytes → ffmpeg stdin from a daemon thread.
        InputStream itemStream;
        try {
            itemStream = item.getBufferedInputStream();
        } catch (Exception e) {
            proc.destroyForcibly();
            return Response.serverError()
                    .entity("{\"error\":\"Cannot open item stream: " + e.getMessage() + "\"}")
                    .build();
        }
        Thread feeder = new Thread(() -> {
            try (OutputStream ffIn = proc.getOutputStream(); InputStream is = itemStream) {
                if (is != null) is.transferTo(ffIn);
            } catch (IOException ignored) {}
        });
        feeder.setDaemon(true);
        feeder.setName("ffmpeg-feeder-" + item.getId());
        feeder.start();

        String outMime = isVideo ? "video/webm" : "audio/webm";
        return Response.ok()
                .header("Content-Type", outMime)
                .header("Cache-Control", "no-store")
                .entity((jakarta.ws.rs.core.StreamingOutput) out -> {
                    try (InputStream ffOut = proc.getInputStream()) {
                        ffOut.transferTo(out);
                    } finally {
                        proc.destroyForcibly();
                    }
                })
                .build();
    }

    /**
     * Parses a single {@code bytes=start-end} range header.
     * Supports suffix-ranges ({@code bytes=-N}) and open-ended ({@code bytes=N-}).
     *
     * @return {@code [start, end]} (inclusive, clamped to {@code [0, total-1]}),
     *         or {@code null} if the range is unsatisfiable.
     */
    private static long[] parseRange(String header, long total) {
        if (total == 0) return null;
        String spec = header.strip();
        if (!spec.startsWith("bytes=")) return null;
        spec = spec.substring(6).strip();

        // Only support single range
        if (spec.contains(",")) return null;

        int dash = spec.indexOf('-');
        if (dash < 0) return null;

        String startStr = spec.substring(0, dash).strip();
        String endStr   = spec.substring(dash + 1).strip();

        long start, end;
        try {
            if (startStr.isEmpty()) {
                // suffix range: bytes=-N → last N bytes
                long suffix = Long.parseLong(endStr);
                start = Math.max(0, total - suffix);
                end   = total - 1;
            } else {
                start = Long.parseLong(startStr);
                end   = endStr.isEmpty() ? total - 1 : Long.parseLong(endStr);
            }
        } catch (NumberFormatException e) {
            return null;
        }

        if (start > end || start >= total) return null;
        end = Math.min(end, total - 1);
        return new long[]{start, end};
    }
}
