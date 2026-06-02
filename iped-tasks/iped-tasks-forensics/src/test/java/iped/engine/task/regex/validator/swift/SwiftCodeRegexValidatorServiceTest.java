package iped.engine.task.regex.validator.swift;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class SwiftCodeRegexValidatorServiceTest {

    private SwiftCodeRegexValidatorService service = new SwiftCodeRegexValidatorService();

    @Test
    public void testValidSwift() {
        String swift = "BACAADAD";
        assertTrue(service.validate(swift));
    }

    @Test
    public void testInvalidSwift() {
        String swift = "XXXXADXX";
        assertFalse(service.validate(swift));
    }

}
