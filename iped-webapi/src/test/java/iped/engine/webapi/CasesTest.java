package iped.engine.webapi;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

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
