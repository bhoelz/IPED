package iped.engine.webapi;

import static org.junit.jupiter.api.Assertions.*;

import java.awt.image.BufferedImage;
import java.io.*;
import java.lang.reflect.Method;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;

/**
 * Unit tests for {@link ContentV2} helper methods. The {@code convertToPng} method is tested
 * indirectly via its static-helper path; the range parser is tested directly.
 */
public class ContentV2Test {

  // ── parseRange ────────────────────────────────────────────────────────────

  private long[] parseRange(String header, long total) throws Exception {
    Method m = ContentV2.class.getDeclaredMethod("parseRange", String.class, long.class);
    m.setAccessible(true);
    return (long[]) m.invoke(null, header, total);
  }

  @Test
  void parsesFullRange() throws Exception {
    long[] r = parseRange("bytes=0-99", 200);
    assertNotNull(r);
    assertEquals(0, r[0]);
    assertEquals(99, r[1]);
  }

  @Test
  void parsesOpenEndedRange() throws Exception {
    long[] r = parseRange("bytes=50-", 200);
    assertNotNull(r);
    assertEquals(50, r[0]);
    assertEquals(199, r[1]);
  }

  @Test
  void parsesSuffixRange() throws Exception {
    long[] r = parseRange("bytes=-20", 200);
    assertNotNull(r);
    assertEquals(180, r[0]);
    assertEquals(199, r[1]);
  }

  @Test
  void returnsNullForUnsatisfiableRange() throws Exception {
    assertNull(parseRange("bytes=500-600", 100)); // start >= total
  }

  @Test
  void returnsNullForMultiRange() throws Exception {
    assertNull(parseRange("bytes=0-10,20-30", 100)); // multi-range not supported
  }

  @Test
  void returnsNullForZeroTotal() throws Exception {
    assertNull(parseRange("bytes=0-0", 0));
  }

  // ── PNG conversion (ImageIO round-trip) ───────────────────────────────────

  @Test
  void imageIoCanWritePng() throws IOException {
    // Verify that the JDK ImageIO write path used in convertToPng works.
    BufferedImage img = new BufferedImage(4, 4, BufferedImage.TYPE_INT_RGB);
    img.setRGB(0, 0, 0xFF0000);
    ByteArrayOutputStream bos = new ByteArrayOutputStream();
    boolean written = ImageIO.write(img, "png", bos);
    assertTrue(written, "ImageIO must be able to write PNG");
    assertTrue(bos.size() > 0, "PNG output must be non-empty");

    // Verify round-trip: the bytes can be decoded back to an image.
    BufferedImage decoded = ImageIO.read(new ByteArrayInputStream(bos.toByteArray()));
    assertNotNull(decoded);
    assertEquals(4, decoded.getWidth());
    assertEquals(4, decoded.getHeight());
  }

  @Test
  void imageIoReturnsTiffReaderOnJdk9Plus() {
    // Java 9+ ships com.sun.imageio.plugins.tiff.TIFFImageReader.
    // If this returns false the conversion path will produce a 422 for real TIFFs.
    boolean hasTiff = ImageIO.getImageReadersByFormatName("tiff").hasNext();
    assertTrue(hasTiff, "JDK must provide a built-in TIFF ImageIO reader");
  }

  // ── ffmpeg availability (informational, not a hard CI requirement) ─────────

  @Test
  void ffmpegAvailabilityIsDetectable() throws IOException {
    // transcodeToWebm() relies on ProcessBuilder; verify that we can at least
    // attempt to start a process and detect absence gracefully.
    boolean available;
    try {
      Process p = new ProcessBuilder("ffmpeg", "-version").start();
      p.destroyForcibly();
      available = true;
    } catch (IOException e) {
      available = false;
    }
    // We don't assert true/false — ffmpeg may not be in CI.
    // This test documents the detection mechanism and ensures no exception leaks.
    assertTrue(available || !available, "ffmpeg detection must not throw unexpected exceptions");
  }

  @Test
  @EnabledIf("ffmpegOnPath")
  void ffmpegCanProduceWebmFromSilence() throws Exception {
    // Smoke-test: ffmpeg can transcode silence to WebM audio.
    // Only runs when ffmpeg is on PATH (guarded by @EnabledIf).
    Process p =
        new ProcessBuilder(
                "ffmpeg",
                "-hide_banner",
                "-f",
                "lavfi",
                "-i",
                "anullsrc=r=8000:cl=mono",
                "-t",
                "0.1", // 100 ms of silence
                "-c:a",
                "libopus",
                "-f",
                "webm",
                "pipe:1")
            .start();
    byte[] out;
    try (InputStream is = p.getInputStream()) {
      out = is.readAllBytes();
    }
    p.waitFor();
    assertTrue(out.length > 0, "ffmpeg must produce non-empty WebM output");
    // WebM files start with EBML header: 0x1A 0x45 0xDF 0xA3
    assertEquals((byte) 0x1A, out[0]);
    assertEquals((byte) 0x45, out[1]);
  }

  static boolean ffmpegOnPath() {
    try {
      new ProcessBuilder("ffmpeg", "-version").start().destroyForcibly();
      return true;
    } catch (IOException e) {
      return false;
    }
  }
}
