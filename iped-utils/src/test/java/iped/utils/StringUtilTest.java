package iped.utils;

import org.junit.jupiter.api.Test;

import java.util.Comparator;

import static org.junit.jupiter.api.Assertions.*;

class StringUtilTest {

    @Test
    void getIgnoreCaseComparator_whenBothNull_thenZero() {
        Comparator<String> cmp = StringUtil.getIgnoreCaseComparator();
        assertEquals(0, cmp.compare(null, null));
    }

    @Test
    void getIgnoreCaseComparator_whenFirstNull_thenNegative() {
        Comparator<String> cmp = StringUtil.getIgnoreCaseComparator();
        assertTrue(cmp.compare(null, "a") < 0);
    }

    @Test
    void getIgnoreCaseComparator_whenSecondNull_thenPositive() {
        Comparator<String> cmp = StringUtil.getIgnoreCaseComparator();
        assertTrue(cmp.compare("a", null) > 0);
    }

    @Test
    void getIgnoreCaseComparator_whenDifferentCase_thenEqual() {
        Comparator<String> cmp = StringUtil.getIgnoreCaseComparator();
        assertEquals(0, cmp.compare("Hello", "hello"));
    }

    @Test
    void getIgnoreCaseComparator_whenLeadingSpaces_thenTrimmed() {
        Comparator<String> cmp = StringUtil.getIgnoreCaseComparator();
        assertEquals(0, cmp.compare("  hello", "hello  "));
    }

    @Test
    void getIgnoreCaseComparator_whenOrdered_thenNegative() {
        Comparator<String> cmp = StringUtil.getIgnoreCaseComparator();
        assertTrue(cmp.compare("apple", "banana") < 0);
    }

    @Test
    void convertCamelCaseToSpaces_whenNull_thenNull() {
        assertNull(StringUtil.convertCamelCaseToSpaces(null));
    }

    @Test
    void convertCamelCaseToSpaces_whenBlank_thenBlank() {
        assertEquals("   ", StringUtil.convertCamelCaseToSpaces("   "));
    }

    @Test
    void convertCamelCaseToSpaces_whenCamelCase_thenSpacesInserted() {
        assertEquals("Camel Case String", StringUtil.convertCamelCaseToSpaces("CamelCaseString"));
    }

    @Test
    void convertCamelCaseToSpaces_whenAlreadySpaced_thenUnchanged() {
        assertEquals("hello world", StringUtil.convertCamelCaseToSpaces("hello world"));
    }

    @Test
    void convertCamelCaseToSpaces_whenAllLowercase_thenUnchanged() {
        assertEquals("lowercase", StringUtil.convertCamelCaseToSpaces("lowercase"));
    }
}
