package iped.carvers.impl;

import iped.carvers.api.CarverType;
import iped.carvers.api.Signature;
import iped.carvers.custom.DERCarver;
import org.apache.commons.codec.DecoderException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Fixture tests for DERCarver: verifies advertised CarverType metadata and
 * that the DER/PKCS#12 header signatures decode without error.
 */
class DERCarverFixtureTest {

    private DERCarver carver;

    @BeforeEach
    void setUp() throws DecoderException {
        carver = new DERCarver();
    }

    @Test
    void exposesOneCarverType() {
        assertNotNull(carver.getCarverTypes());
        assertEquals(1, carver.getCarverTypes().length);
    }

    @Test
    void mimeTypeIsPkixCert() {
        assertEquals("application/pkix-cert",
                carver.getCarverTypes()[0].getMimeType().toString());
    }

    @Test
    void nameIsDer() {
        assertEquals("DER", carver.getCarverTypes()[0].getName());
    }

    @Test
    void sizeBoundsArePresent() {
        CarverType ct = carver.getCarverTypes()[0];
        assertNotNull(ct.getMinLength());
        assertNotNull(ct.getMaxLength());
        assertTrue(ct.getMinLength() > 0);
        assertTrue(ct.getMaxLength() > ct.getMinLength());
    }

    @Test
    void hasAtLeastTwoHeaderSignatures() {
        CarverType ct = carver.getCarverTypes()[0];
        // DERCarver adds two patterns: \30\82??\30\82 and \30\83???\30\83
        long headers = ct.getSignatures().stream().filter(Signature::isHeader).count();
        assertTrue(headers >= 2, "expected at least two header signatures for DER/PKCS#12");
    }

    @Test
    void carverClassIsSet() {
        assertEquals(DERCarver.class.getName(),
                carver.getCarverTypes()[0].getCarverClass());
    }

    @Test
    void headerSignaturesHavePositiveLength() {
        CarverType ct = carver.getCarverTypes()[0];
        ct.getSignatures().stream()
                .filter(Signature::isHeader)
                .forEach(sig -> assertTrue(sig.getLength() > 0,
                        "header signature must have positive length: " + sig.getSigString()));
    }
}
