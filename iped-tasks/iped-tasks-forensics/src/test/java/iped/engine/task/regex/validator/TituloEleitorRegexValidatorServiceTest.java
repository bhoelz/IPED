package iped.engine.task.regex.validator;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class TituloEleitorRegexValidatorServiceTest {

    private TituloEleitorRegexValidatorService service = new TituloEleitorRegexValidatorService();

    @Test
    public void testValidTituloEleitor() {
        String tituloEleitor = "270343380159";
        assertTrue(service.validate(tituloEleitor));
    }

    @Test
    public void testInvalidTituloEleitor() {
        String tituloEleitor = "270343380151";
        assertFalse(service.validate(tituloEleitor));
    }

}
