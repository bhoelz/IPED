package iped.engine.datasource.ad1;

import static org.junit.jupiter.api.Assertions.*;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Date;
import org.junit.jupiter.api.Test;

class FileHeaderTest {

  @Test
  void shouldExposeFlagsAndPathAndChunks() {
    FileHeader header = new FileHeader(null, null);
    header.objectType = 0x06;
    header.childAddress = 2;
    header.path = "/root/a";
    header.objectName = "a.txt";
    header.setObjectSizeBytes(123L);
    header.addChunk(1, 9);

    assertTrue(header.hasChildren());
    assertTrue(header.isDirectory());
    assertTrue(header.isDeleted());
    assertEquals("/root/a", header.getFilePath());
    assertEquals("a.txt", header.getFileName());
    assertEquals(123L, header.getFileSize());
    assertEquals(1, header.chunkList.size());
    assertFalse(header.isEncrypted());
  }

  @Test
  void shouldParseKnownDatesAndIgnoreInvalid() {
    FileHeader header = new FileHeader(null, null);
    header.propertiesMap.put(7, new Property("20240101T000000"));
    header.propertiesMap.put(8, new Property("bad"));
    header.propertiesMap.put(9, new Property("20240102T000000"));
    header.propertiesMap.put(40962, new Property("20240103T000000"));

    Date a = header.getATime();
    Date c = header.getCTime();
    Date m = header.getMTime();
    Date r = header.getRTime();

    assertNotNull(a);
    assertNull(c);
    assertNotNull(m);
    assertNotNull(r);
  }

  @Test
  void ad1ExtractorShouldFailForMissingFile() throws IOException {
    File missing = Files.createTempDirectory("ad1").resolve("missing.ad1").toFile();
    assertFalse(missing.exists());
    try {
      new AD1Extractor(missing);
    } catch (IOException e) {
      assertTrue(e.getMessage().contains("nao encontrado"));
      return;
    }
    throw new AssertionError("Expected IOException");
  }
}
