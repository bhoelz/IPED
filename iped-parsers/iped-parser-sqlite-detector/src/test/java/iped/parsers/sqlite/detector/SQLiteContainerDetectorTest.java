package iped.parsers.sqlite.detector;

import java.io.IOException;
import java.io.InputStream;
import junit.framework.TestCase;
import org.apache.tika.exception.TikaException;
import org.apache.tika.io.TikaInputStream;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.mime.MediaType;
import org.junit.Test;
import org.xml.sax.SAXException;

public class SQLiteContainerDetectorTest extends TestCase {
  private static final String GDRIVE_ACCOUNT_INFO = "application/x-gdrive-account-info";
  private static final String WIN10_TIMELINE = "application/x-win10-timeline";
  private static final String SKYPE_MIME = "application/sqlite-skype";
  private static final String SKYPE_MIME_V12 = "application/sqlite-skype-v12";
  private static final String GDRIVE_CLOUD_GRAPH = "application/x-gdrive-cloud-graph";
  private static final String CHROME_SQLITE = "application/x-chrome-sqlite";
  private static final String GDRIVE_SNAPSHOT = "application/x-gdrive-snapshot";
  private static final String WA_MSG_STORE = "application/x-whatsapp-db";

  private static InputStream getStream(String name) {
    return Thread.currentThread().getContextClassLoader().getResourceAsStream(name);
  }

  @Test
  public void testSQLiteContainerDetectorGlobalDB()
      throws IOException, SAXException, TikaException {

    SQLiteContainerDetector detector = new SQLiteContainerDetector();
    Metadata metadata = new Metadata();
    try (InputStream stream = getStream("test-files/test_global.db")) {
      TikaInputStream tis = TikaInputStream.get(stream);
      MediaType assertion = detector.detect(tis, metadata);
      assertEquals(assertion.toString(), GDRIVE_ACCOUNT_INFO);
    }
  }

  @Test
  public void testSQLiteContainerDetectorActivitiesCache()
      throws IOException, SAXException, TikaException {

    SQLiteContainerDetector detector = new SQLiteContainerDetector();
    Metadata metadata = new Metadata();
    try (InputStream stream = getStream("test-files/test_activitiesCache.db")) {
      TikaInputStream tis = TikaInputStream.get(stream);
      MediaType assertion = detector.detect(tis, metadata);
      assertEquals(assertion.toString(), WIN10_TIMELINE);
    }
  }

  @Test
  public void testSQLiteContainerDetectorSkypeMain()
      throws IOException, SAXException, TikaException {

    SQLiteContainerDetector detector = new SQLiteContainerDetector();
    Metadata metadata = new Metadata();
    try (InputStream stream = getStream("test-files/test_skypeMain.db")) {

      TikaInputStream tis = TikaInputStream.get(stream);
      MediaType assertion = detector.detect(tis, metadata);
      assertEquals(assertion.toString(), SKYPE_MIME);
    }
  }

  @Test
  public void testSQLiteContainerDetectorSkype() throws IOException, SAXException, TikaException {

    SQLiteContainerDetector detector = new SQLiteContainerDetector();
    Metadata metadata = new Metadata();
    try (InputStream stream = getStream("test-files/test_skypeS4lStreeguil1.db")) {

      TikaInputStream tis = TikaInputStream.get(stream);
      MediaType assertion = detector.detect(tis, metadata);
      assertEquals(assertion.toString(), SKYPE_MIME_V12);
    }
  }

  @Test
  public void testSQLiteContainerDetectorCloudGraph()
      throws IOException, SAXException, TikaException {

    SQLiteContainerDetector detector = new SQLiteContainerDetector();
    Metadata metadata = new Metadata();
    try (InputStream stream = getStream("test-files/test_cloudGraph.db")) {
      TikaInputStream tis = TikaInputStream.get(stream);
      MediaType assertion = detector.detect(tis, metadata);
      assertEquals(assertion.toString(), GDRIVE_CLOUD_GRAPH);
    }
  }

  @Test
  public void testSQLiteContainerDetectorChrome() throws IOException, SAXException, TikaException {

    SQLiteContainerDetector detector = new SQLiteContainerDetector();
    Metadata metadata = new Metadata();
    try (InputStream stream = getStream("test-files/test_historyChrome")) {
      TikaInputStream tis = TikaInputStream.get(stream);
      MediaType assertion = detector.detect(tis, metadata);
      assertEquals(assertion.toString(), CHROME_SQLITE);
    }
  }

  @Test
  public void testSQLiteContainerDetectorGDriveSnapshot()
      throws IOException, SAXException, TikaException {

    SQLiteContainerDetector detector = new SQLiteContainerDetector();
    Metadata metadata = new Metadata();
    try (InputStream stream = getStream("test-files/test_snapshot.db")) {
      TikaInputStream tis = TikaInputStream.get(stream);
      MediaType assertion = detector.detect(tis, metadata);
      assertEquals(assertion.toString(), GDRIVE_SNAPSHOT);
    }
  }

  @Test
  public void testSQLiteContainerDetectorWhatsappMsgStore()
      throws IOException, SAXException, TikaException {

    SQLiteContainerDetector detector = new SQLiteContainerDetector();
    Metadata metadata = new Metadata();
    try (InputStream stream = getStream("test-files/test_whatsAppMsgStore.db")) {
      TikaInputStream tis = TikaInputStream.get(stream);
      MediaType assertion = detector.detect(tis, metadata);
      assertEquals(assertion.toString(), WA_MSG_STORE);
    }
  }
}
