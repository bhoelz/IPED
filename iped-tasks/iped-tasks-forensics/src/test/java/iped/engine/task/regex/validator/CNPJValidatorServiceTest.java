package iped.engine.task.regex.validator;

import org.junit.Test;

import static org.junit.Assert.*;

public class CNPJValidatorServiceTest {

    private CNPJRegexValidatorService service = new CNPJRegexValidatorService();

    @Test
    public void testInvalidCNPJ() {
        String cnpj = "25783925000111";
        assertFalse(service.validate(cnpj));
    }

    @Test
    public void testValidCNPJ() {
        String cnpj = "25783925000187";
        assertTrue(service.validate(cnpj));
    }

    @Test
    public void testFormatUnformattedCNPJ() {
        String cnpj = "25783925000187";
        assertEquals("25.783.925/0001-87", service.format(cnpj));
    }

    @Test
    public void testFormatPartiallyormattedCNPJ() {
        String cnpj = "257839250001-87";
        assertEquals("25.783.925/0001-87", service.format(cnpj));
    }

}
