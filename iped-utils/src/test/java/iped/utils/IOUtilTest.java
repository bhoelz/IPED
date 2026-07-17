package iped.utils;

import static org.junit.jupiter.api.Assertions.*;

import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class IOUtilTest {

  @TempDir Path tempDir;

  // --- isDangerousExtension ---

  @Test
  void isDangerousExtension_whenExe_thenTrue() {
    assertTrue(IOUtil.isDangerousExtension("exe"));
  }

  @Test
  void isDangerousExtension_whenBat_thenTrue() {
    assertTrue(IOUtil.isDangerousExtension("BAT"));
  }

  @Test
  void isDangerousExtension_whenPs1_thenTrue() {
    assertTrue(IOUtil.isDangerousExtension("PS1"));
  }

  @Test
  void isDangerousExtension_whenJar_thenTrue() {
    assertTrue(IOUtil.isDangerousExtension("jar"));
  }

  @Test
  void isDangerousExtension_whenTxt_thenFalse() {
    assertFalse(IOUtil.isDangerousExtension("txt"));
  }

  @Test
  void isDangerousExtension_whenPdf_thenFalse() {
    assertFalse(IOUtil.isDangerousExtension("pdf"));
  }

  @Test
  void isDangerousExtension_whenNull_thenFalse() {
    assertFalse(IOUtil.isDangerousExtension(null));
  }

  // --- getExtension ---

  @Test
  void getExtension_whenHasExtension_thenReturnsIt() {
    assertEquals("pdf", IOUtil.getExtension(new File("document.pdf")));
  }

  @Test
  void getExtension_whenNoExtension_thenEmpty() {
    assertEquals("", IOUtil.getExtension(new File("README")));
  }

  @Test
  void getExtension_whenMultipleDots_thenLastExtension() {
    assertEquals("gz", IOUtil.getExtension(new File("archive.tar.gz")));
  }

  // --- getValidFilename ---

  @Test
  void getValidFilename_whenHasInvalidChars_thenReplaced() {
    String result = IOUtil.getValidFilename("file:name?.txt");
    assertFalse(result.contains(":"));
    assertFalse(result.contains("?"));
    assertTrue(result.endsWith(".txt"));
  }

  @Test
  void getValidFilename_whenReservedName_thenPrefixed() {
    String result = IOUtil.getValidFilename("CON");
    assertFalse(result.equalsIgnoreCase("CON"));
    assertTrue(result.startsWith("1"));
  }

  @Test
  void getValidFilename_whenReservedNameWithExtension_thenPrefixed() {
    String result = IOUtil.getValidFilename("NUL.txt");
    assertFalse(result.toUpperCase().startsWith("NUL."));
  }

  @Test
  void getValidFilename_whenTooLong_thenTruncated() {
    String longName = "a".repeat(200) + ".txt";
    String result = IOUtil.getValidFilename(longName);
    assertTrue(result.length() <= 160);
    assertTrue(result.endsWith(".txt"));
  }

  @Test
  void getValidFilename_whenTrailingDot_thenRemoved() {
    String result = IOUtil.getValidFilename("file.");
    assertFalse(result.endsWith("."));
  }

  @Test
  void getValidFilename_whenTrailingSpace_thenRemoved() {
    String result = IOUtil.getValidFilename("file   ");
    assertFalse(result.endsWith(" "));
  }

  // --- isDiskFull ---

  @Test
  void isDiskFull_whenNull_thenFalse() {
    assertFalse(IOUtil.isDiskFull(null));
  }

  @Test
  void isDiskFull_whenNullMessage_thenFalse() {
    assertFalse(IOUtil.isDiskFull(new IOException((String) null)));
  }

  @Test
  void isDiskFull_whenNoSpaceLeft_thenTrue() {
    assertTrue(IOUtil.isDiskFull(new IOException("No space left on device")));
  }

  @Test
  void isDiskFull_whenNotEnoughSpace_thenTrue() {
    assertTrue(IOUtil.isDiskFull(new IOException("There is not enough space on the disk")));
  }

  @Test
  void isDiskFull_whenUnrelatedError_thenFalse() {
    assertFalse(IOUtil.isDiskFull(new IOException("Permission denied")));
  }

  // --- closeQuietly ---

  @Test
  void closeQuietly_whenNull_thenNoException() {
    assertDoesNotThrow(() -> IOUtil.closeQuietly(null));
  }

  @Test
  void closeQuietly_whenThrowingCloseable_thenNoException() {
    Closeable boom =
        () -> {
          throw new IOException("kaboom");
        };
    assertDoesNotThrow(() -> IOUtil.closeQuietly(boom));
  }

  @Test
  void closeQuietly_whenValidStream_thenClosed() throws Exception {
    File f = tempDir.resolve("dummy.txt").toFile();
    f.createNewFile();
    FileInputStream fis = new FileInputStream(f);
    IOUtil.closeQuietly(fis);
    // Reading from a closed stream should throw
    assertThrows(IOException.class, fis::read);
  }
}
