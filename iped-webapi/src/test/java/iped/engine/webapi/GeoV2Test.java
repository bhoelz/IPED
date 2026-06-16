package iped.engine.webapi;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link GeoV2} helper methods (no running server required).
 */
class GeoV2Test {

    // ── buildFeature ──────────────────────────────────────────────────────────

    private String buildFeature(String featureId, String sourceId, int docId,
                                String name, String locValue,
                                Map<String, List<String>> meta) throws Exception {
        Method m = GeoV2.class.getDeclaredMethod(
                "buildFeature", String.class, String.class, int.class,
                String.class, String.class, Map.class);
        m.setAccessible(true);
        return (String) m.invoke(null, featureId, sourceId, docId, name, locValue, meta);
    }

    @Test
    void buildFeatureProducesValidGeoJsonPoint() throws Exception {
        Map<String, List<String>> meta = Map.of(
                "dcterms:created", List.of("2024-01-15T10:30:00Z")
        );
        String json = buildFeature("src:42:0", "src", 42, "photo.jpg",
                "-22.906;-43.172", meta);

        assertNotNull(json, "feature must not be null for valid coordinates");
        assertTrue(json.contains("\"type\":\"Feature\""));
        assertTrue(json.contains("\"type\":\"Point\""));
        // GeoJSON coordinates are [lon, lat]
        assertTrue(json.contains("-43.172,-22.906"));
        assertTrue(json.contains("\"name\":\"photo.jpg\""));
        assertTrue(json.contains("\"docId\":42"));
        assertTrue(json.contains("\"sourceId\":\"src\""));
        assertTrue(json.contains("2024-01-15T10:30:00Z"));
    }

    @Test
    void buildFeatureIncludesAltitudeWhenPresent() throws Exception {
        Map<String, List<String>> meta = Map.of(
                "common:altitude", List.of("890.5")
        );
        String json = buildFeature("src:1:0", "src", 1, "gps.jpg",
                "-10.0;20.0", meta);

        assertNotNull(json);
        // altitude appended as third coordinate element
        assertTrue(json.contains("20.0,-10.0,890.5"), "altitude must be third coordinate: " + json);
    }

    @Test
    void buildFeatureReturnsNullForUnparseableCoordinates() throws Exception {
        String json = buildFeature("src:1:0", "src", 1, "bad.jpg",
                "NOT_A_NUMBER;ALSO_BAD", Map.of());
        assertNull(json, "unparseable coordinates must yield null");
    }

    @Test
    void buildFeatureReturnsNullForMissingDelimiter() throws Exception {
        String json = buildFeature("src:1:0", "src", 1, "bad.jpg",
                "51.5074", Map.of()); // no semicolon
        assertNull(json, "missing semicolon must yield null");
    }

    @Test
    void buildFeatureEscapesJsonSpecialCharsInName() throws Exception {
        String json = buildFeature("src:2:0", "src", 2,
                "file\"name\\path", "51.0;0.0", Map.of());

        assertNotNull(json);
        assertTrue(json.contains("file\\\"name\\\\path"),
                "quotes and backslashes must be escaped in JSON string: " + json);
    }

    // ── startFeatureCollection ────────────────────────────────────────────────

    private StringBuilder startFeatureCollection(long total, boolean truncated) throws Exception {
        Method m = GeoV2.class.getDeclaredMethod("startFeatureCollection", long.class, boolean.class);
        m.setAccessible(true);
        return (StringBuilder) m.invoke(null, total, truncated);
    }

    @Test
    void startFeatureCollectionContainsCorrectMetadata() throws Exception {
        StringBuilder sb = startFeatureCollection(42, false);
        String s = sb.toString();
        assertTrue(s.contains("\"type\":\"FeatureCollection\""));
        assertTrue(s.contains("\"total\":42"));
        assertTrue(s.contains("\"truncated\":false"));
        assertTrue(s.contains("\"features\":["));
    }

    @Test
    void startFeatureCollectionMarksTruncated() throws Exception {
        String s = startFeatureCollection(100_000, true).toString();
        assertTrue(s.contains("\"truncated\":true"));
    }

    // ── GeoDataService (iped-geo) coordinate parsing ──────────────────────────

    @Test
    void locationFormatRoundtrip() {
        // Documents the "lat;lon" format used by ExtraProperties.LOCATIONS
        String stored = "-22.906;-43.172";
        String[] parts = stored.split(";");
        assertEquals(2, parts.length);
        double lat = Double.parseDouble(parts[0].trim());
        double lon = Double.parseDouble(parts[1].trim());
        assertEquals(-22.906, lat, 1e-6);
        assertEquals(-43.172, lon, 1e-6);
    }

    @Test
    void geoJsonCoordinateOrderIsLonLat() throws Exception {
        // GeoJSON spec: [longitude, latitude] — verify our output matches
        String json = buildFeature("src:1:0", "src", 1, "img.jpg",
                "-22.906;-43.172", Map.of()); // lat=-22.906, lon=-43.172
        assertNotNull(json);
        // coordinates array must be [lon, lat] → [-43.172, -22.906]
        int coordIdx = json.indexOf("\"coordinates\":[");
        assertTrue(coordIdx >= 0);
        String coords = json.substring(coordIdx + "\"coordinates\":[".length());
        coords = coords.substring(0, coords.indexOf(']'));
        String[] vals = coords.split(",");
        assertEquals(-43.172, Double.parseDouble(vals[0].trim()), 1e-6, "first coord must be longitude");
        assertEquals(-22.906, Double.parseDouble(vals[1].trim()), 1e-6, "second coord must be latitude");
    }
}
