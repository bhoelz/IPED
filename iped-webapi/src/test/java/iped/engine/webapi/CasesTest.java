package iped.engine.webapi;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;

public class CasesTest {

    @BeforeEach
    void setUp() {
    }

    @Test
    void testCasesResourceExists() {
        Cases cases = new Cases();
        assertNotNull(cases, "Cases resource should exist");
    }
}
