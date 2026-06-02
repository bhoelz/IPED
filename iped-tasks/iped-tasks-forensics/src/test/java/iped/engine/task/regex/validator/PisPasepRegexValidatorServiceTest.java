package iped.engine.task.regex.validator;

import org.junit.Test;

import static org.junit.Assert.*;

public class PisPasepRegexValidatorServiceTest {

    private PisPasepRegexValidatorService service = new PisPasepRegexValidatorService();

    @Test
    public void testValidPisPasep() {
        String pisPasep = "89206075873";
        assertTrue(service.validate(pisPasep));
    }

    @Test
    public void testInvalidPisPasep() {
        String pisPasep = "89206075871";
        assertFalse(service.validate(pisPasep));
    }

    @Test
    public void testFormatUnformattedPisPasep() {
        String pisPasep = "42313629124";
        assertEquals("423.13629.12-4", service.format(pisPasep));
    }

    @Test
    public void testFormatPartiallyormattedPisPasep() {
        String cnpj = "4231362912-4";
        assertEquals("423.13629.12-4", service.format(cnpj));
    }

}
