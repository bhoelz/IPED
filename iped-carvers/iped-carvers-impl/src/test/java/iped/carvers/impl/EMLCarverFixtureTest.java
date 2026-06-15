package iped.carvers.impl;

import iped.carvers.api.CarverType;
import iped.carvers.api.Signature;
import org.apache.commons.codec.DecoderException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Fixture tests for EML carver configuration: EMLCarver receives its CarverType
 * externally (from CarverConfig.toml / XMLCarverConfiguration), so these tests
 * verify that the expected header/footer signatures decode correctly via the
 * CarverType API rather than via a carver constructor.
 */
class EMLCarverFixtureTest {

    private CarverType buildEmlCarverType() throws DecoderException {
        CarverType ct = new CarverType();
        ct.setName("EML");
        ct.addHeader("Received: by");
        ct.addHeader("Received: from");
        ct.addHeader("Message-ID:\\20<");
        ct.addHeader("Return-Path:\\20");
        ct.addFooter("--\\0d\\0a");
        ct.setMinLength(1_000L);
        ct.setMaxLength(10_000_000L);
        return ct;
    }

    @Test
    void fourHeadersAndOneFooter() throws DecoderException {
        CarverType ct = buildEmlCarverType();
        long headers = ct.getSignatures().stream().filter(Signature::isHeader).count();
        long footers = ct.getSignatures().stream().filter(Signature::isFooter).count();
        assertEquals(4, headers, "expected four EML header patterns");
        assertEquals(1, footers, "expected one EML footer pattern");
    }

    @Test
    void allHeaderSignaturesHavePositiveLength() throws DecoderException {
        CarverType ct = buildEmlCarverType();
        ct.getSignatures().stream()
                .filter(Signature::isHeader)
                .forEach(sig -> assertTrue(sig.getLength() > 0,
                        "header '" + sig.getSigString() + "' must have positive decoded length"));
    }

    @Test
    void crlfFooterDecodesTo4Bytes() throws DecoderException {
        // "--\r\n" = 4 bytes
        CarverType ct = buildEmlCarverType();
        Signature footer = ct.getSignatures().stream()
                .filter(Signature::isFooter)
                .findFirst()
                .orElseThrow(() -> new AssertionError("no footer"));
        assertEquals(4, footer.getLength(), "EML footer '--\\r\\n' must decode to 4 bytes");
    }

    @Test
    void sizeLimitsAreSet() throws DecoderException {
        CarverType ct = buildEmlCarverType();
        assertNotNull(ct.getMinLength());
        assertNotNull(ct.getMaxLength());
        assertTrue(ct.getMaxLength() > ct.getMinLength());
    }

    @Test
    void hasFooterFlagIsSet() throws DecoderException {
        CarverType ct = buildEmlCarverType();
        assertTrue(ct.hasFooter(), "hasFooter() must return true after addFooter()");
    }
}
