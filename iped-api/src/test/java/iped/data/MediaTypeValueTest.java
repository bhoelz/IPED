package iped.data;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import org.junit.jupiter.api.Test;

class MediaTypeValueTest {

    @Test
    void of_whenValueProvided_thenExposesSameValue() {
        MediaTypeValue value = MediaTypeValue.of("application/x-test");

        assertEquals("application/x-test", value.value());
        assertEquals("application/x-test", value.toString());
    }

    @Test
    void equalsAndHashCode_whenSameValue_thenMatch() {
        MediaTypeValue a = MediaTypeValue.of("a");
        MediaTypeValue b = MediaTypeValue.of("a");
        MediaTypeValue c = MediaTypeValue.of("b");

        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());
        assertNotEquals(a, c);
    }
}

