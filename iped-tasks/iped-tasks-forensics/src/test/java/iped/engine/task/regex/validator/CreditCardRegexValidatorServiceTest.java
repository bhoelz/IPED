package iped.engine.task.regex.validator;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class CreditCardRegexValidatorServiceTest {

    private CreditCardRegexValidatorService service = new CreditCardRegexValidatorService();

    @Test
    public void testValidCreditCard() {
        String creditCard = "4369811367191811";
        assertTrue(service.validate(creditCard));
    }

    @Test
    public void testInvalidCreditCard() {
        String creditCard = "4369811367191812";
        assertFalse(service.validate(creditCard));
    }

}
