package iped.engine.webapi;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;

public class StatsTest {

    @BeforeEach
    void setUp() {
    }

    @Test
    void testStatsResourceExists() {
        Stats stats = new Stats();
        assertNotNull(stats, "Stats resource should exist");
    }
}
