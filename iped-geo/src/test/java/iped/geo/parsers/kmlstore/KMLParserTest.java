package iped.geo.parsers.kmlstore;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Point;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.opengis.feature.simple.SimpleFeature;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class KMLParserTest {

    @TempDir
    Path tempDir;

    private File kmlFile(String body) throws Exception {
        String kml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
                + "<kml xmlns=\"http://www.opengis.net/kml/2.2\">\n"
                + "<Document>\n" + body + "\n</Document>\n</kml>";
        Path file = tempDir.resolve("test.kml");
        Files.write(file, kml.getBytes(StandardCharsets.UTF_8));
        return file.toFile();
    }

    @Test
    public void parsesCoordinateWithElevation() {
        Coordinate c = KMLParser.parseCoordinate("-46.6333,-23.5505,760");
        assertEquals(-46.6333, c.x, 1e-9);
        assertEquals(-23.5505, c.y, 1e-9);
        assertEquals(760, c.z, 1e-9);
    }

    @Test
    public void parsesCoordinateWithoutElevation() {
        Coordinate c = KMLParser.parseCoordinate("10.5,20.25");
        assertEquals(10.5, c.x, 1e-9);
        assertEquals(20.25, c.y, 1e-9);
        assertTrue(Double.isNaN(c.z));
    }

    @Test
    public void parsesCoordinateWithCustomTokens() {
        Coordinate c = KMLParser.parseCoordinate("10.5 20.25 5", " ,");
        assertEquals(10.5, c.x, 1e-9);
        assertEquals(20.25, c.y, 1e-9);
        assertEquals(5, c.z, 1e-9);
    }

    @Test
    public void parsesPointPlacemark() throws Exception {
        File file = kmlFile(
                "<Placemark>"
                        + "<name>Crime scene</name>"
                        + "<description>first hit</description>"
                        + "<Point><coordinates>-46.6333,-23.5505,0</coordinates></Point>"
                        + "</Placemark>");

        List<Object> features = KMLParser.parse(file);

        assertEquals(1, features.size());
        SimpleFeature feature = assertInstanceOf(SimpleFeature.class, features.get(0));
        assertEquals("Crime scene", feature.getAttribute("name"));
        assertEquals("first hit", feature.getAttribute("description"));
        Point point = assertInstanceOf(Point.class, feature.getDefaultGeometry());
        assertEquals(-46.6333, point.getX(), 1e-9);
        assertEquals(-23.5505, point.getY(), 1e-9);
    }

    @Test
    public void parsesTimeStamp() throws Exception {
        File file = kmlFile(
                "<Placemark>"
                        + "<name>P</name>"
                        + "<TimeStamp><when>2024-03-15T10:00:00Z</when></TimeStamp>"
                        + "<Point><coordinates>1,2</coordinates></Point>"
                        + "</Placemark>");

        List<Object> features = KMLParser.parse(file);
        SimpleFeature feature = (SimpleFeature) features.get(0);
        assertEquals("2024-03-15T10:00:00Z", feature.getAttribute("timestamp"));
    }

    @Test
    public void parsesFolderWithNestedPlacemarks() throws Exception {
        File file = kmlFile(
                "<Folder>"
                        + "<name>Route A</name>"
                        + "<Placemark><name>P1</name><Point><coordinates>1,1</coordinates></Point></Placemark>"
                        + "<Placemark><name>P2</name><Point><coordinates>2,2</coordinates></Point></Placemark>"
                        + "</Folder>");

        List<Object> features = KMLParser.parse(file);

        assertEquals(1, features.size());
        Folder folder = assertInstanceOf(Folder.class, features.get(0));
        assertEquals("Route A", folder.getName());
        assertEquals(2, folder.getFeatures().size());
        assertTrue(folder.getFeatures().get(0) instanceof SimpleFeature);
    }

    @Test
    public void folderWithIpedTrackExtendedDataIsTrack() throws Exception {
        // mirrors the output of gpxtokml.xsl: only the root kml element is in
        // the KML namespace, Folder/ExtendedData/Data are in no namespace
        String kml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
                + "<kml xmlns=\"http://www.opengis.net/kml/2.2\">"
                + "<Document xmlns=\"\">"
                + "<Folder>"
                + "<name>Track</name>"
                + "<ExtendedData><Data name=\"iped.geo.track\"><value>track</value></Data></ExtendedData>"
                + "</Folder>"
                + "</Document></kml>";
        Path file = tempDir.resolve("track.kml");
        Files.write(file, kml.getBytes(StandardCharsets.UTF_8));

        List<Object> features = KMLParser.parse(file.toFile());
        Folder folder = (Folder) features.get(0);
        assertTrue(folder.isTrack());
    }

    @Test
    public void plainFolderIsNotTrack() throws Exception {
        File file = kmlFile("<Folder><name>F</name></Folder>");
        Folder folder = (Folder) KMLParser.parse(file).get(0);
        assertEquals("F", folder.getName());
        assertTrue(!folder.isTrack());
    }

    @Test
    public void parsesLineStringPlacemark() throws Exception {
        File file = kmlFile(
                "<Placemark>"
                        + "<name>Path</name>"
                        + "<LineString><coordinates>1,1 2,2 3,3</coordinates></LineString>"
                        + "</Placemark>");

        SimpleFeature feature = (SimpleFeature) KMLParser.parse(file).get(0);
        com.vividsolutions.jts.geom.LineString line = assertInstanceOf(com.vividsolutions.jts.geom.LineString.class,
                feature.getDefaultGeometry());
        assertEquals(3, line.getNumPoints());
    }

    @Test
    public void placemarkWithoutGeometryHasNullGeometry() throws Exception {
        File file = kmlFile("<Placemark><name>NoGeo</name></Placemark>");
        SimpleFeature feature = (SimpleFeature) KMLParser.parse(file).get(0);
        assertEquals("NoGeo", feature.getAttribute("name"));
        assertNull(feature.getDefaultGeometry());
    }
}
