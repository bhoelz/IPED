package iped.engine.webapi.spi;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

class SpiValueObjectsTest {

    // ── DocRef ──────────────────────────────────────────────────────────────

    @Test
    void docRef_constructorAndGetters_roundTrip() {
        DocRef ref = new DocRef("src1", 42);
        assertEquals("src1", ref.getSource());
        assertEquals(42, ref.getId());
    }

    @Test
    void docRef_nullSource_storedAndReturned() {
        DocRef ref = new DocRef(null, 0);
        assertNull(ref.getSource());
        assertEquals(0, ref.getId());
    }

    // ── SourceDescriptor ────────────────────────────────────────────────────

    @Test
    void sourceDescriptor_constructorAndGetters_roundTrip() {
        SourceDescriptor sd = new SourceDescriptor("id-1", "/evidence/disk.E01");
        assertEquals("id-1", sd.getId());
        assertEquals("/evidence/disk.E01", sd.getPath());
    }

    @Test
    void sourceDescriptor_nullFields_storedAndReturned() {
        SourceDescriptor sd = new SourceDescriptor(null, null);
        assertNull(sd.getId());
        assertNull(sd.getPath());
    }

    // ── RenditionDescriptor ─────────────────────────────────────────────────

    @Test
    void renditionDescriptor_constructorAndGetters_roundTrip() {
        RenditionDescriptor rd = new RenditionDescriptor("html", "/api/renditions/1/html");
        assertEquals("html", rd.getKind());
        assertEquals("/api/renditions/1/html", rd.getUrl());
    }

    @Test
    void renditionDescriptor_nullFields_storedAndReturned() {
        RenditionDescriptor rd = new RenditionDescriptor(null, null);
        assertNull(rd.getKind());
        assertNull(rd.getUrl());
    }
}
