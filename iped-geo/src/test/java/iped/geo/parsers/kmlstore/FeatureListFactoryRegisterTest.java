package iped.geo.parsers.kmlstore;

import iped.geo.parsers.GeofileParser;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;

public class FeatureListFactoryRegisterTest {

    @Test
    public void resolvesKmlFactory() {
        FeatureListFactory factory = FeatureListFactoryRegister.getFeatureList(GeofileParser.KML_MIME.toString());
        assertInstanceOf(KMLFeatureListFactory.class, factory);
    }

    @Test
    public void resolvesGpxFactory() {
        FeatureListFactory factory = FeatureListFactoryRegister.getFeatureList(GeofileParser.GPX_MIME.toString());
        assertInstanceOf(GPXFeatureListFactory.class, factory);
    }

    @Test
    public void unknownMimeTypeReturnsNull() {
        assertNull(FeatureListFactoryRegister.getFeatureList("application/pdf"));
    }
}
