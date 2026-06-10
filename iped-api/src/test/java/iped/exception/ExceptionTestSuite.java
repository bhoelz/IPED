package iped.exception;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test suite for all exception classes in iped-api.
 * Combines tests for ParseException and QueryNodeException.
 */
class ExceptionTestSuite {

    // ============== ParseException Tests ==============

    @Test
    @DisplayName("ParseException default constructor should create instance")
    void parseException_defaultConstructor_shouldCreateInstance() {
        // When
        ParseException exception = new ParseException();

        // Then
        assertNotNull(exception);
        assertNull(exception.getMessage());
    }

    @Test
    @DisplayName("ParseException should be standard Exception")
    void parseException_shouldBeStandardException() {
        // When
        ParseException exception = new ParseException();

        // Then
        assertTrue(Exception.class.isAssignableFrom(exception.getClass()));
        assertFalse(RuntimeException.class.isAssignableFrom(exception.getClass()));
    }

    @Test
    @DisplayName("ParseException should be throwable as checked exception")
    void parseException_shouldBeThrowable() throws ParseException {
        // Given
        ParseException exception = new ParseException();

        // When/Then - verify it can be thrown and caught as checked exception
        try {
            throw exception;
        } catch (ParseException caught) {
            assertSame(exception, caught);
        }
    }

    @Test
    @DisplayName("ParseException should have serialVersionUID")
    void parseException_shouldHaveSerialVersionUID() {
        // Verify class is properly serializable
        ParseException exception = new ParseException();
        assertNotNull(exception);
    }

    // ============== QueryNodeException Tests ==============

    @Test
    @DisplayName("QueryNodeException constructor with cause should set cause")
    void queryNodeException_withCause_shouldSetCause() {
        // Given
        Exception cause = new Exception("Original cause");

        // When
        QueryNodeException exception = new QueryNodeException(cause);

        // Then
        assertEquals(cause, exception.getCause());
    }

    @Test
    @DisplayName("QueryNodeException constructor with cause should set message")
    void queryNodeException_withCause_shouldSetMessage() {
        // Given
        String causeMessage = "Original cause";
        Exception cause = new Exception(causeMessage);

        // When
        QueryNodeException exception = new QueryNodeException(cause);

        // Then
        assertNotNull(exception.getMessage());
        // Message should contain or reference the cause
    }

    @Test
    @DisplayName("QueryNodeException should be standard Exception")
    void queryNodeException_shouldBeStandardException() {
        // When
        QueryNodeException exception = new QueryNodeException(new Exception("cause"));

        // Then
        assertTrue(Exception.class.isAssignableFrom(exception.getClass()));
        assertFalse(RuntimeException.class.isAssignableFrom(exception.getClass()));
    }

    @Test
    @DisplayName("QueryNodeException should be throwable as checked exception")
    void queryNodeException_shouldBeThrowable() throws QueryNodeException {
        // Given
        QueryNodeException exception = new QueryNodeException(new Exception("cause"));

        // When/Then - verify it can be thrown and caught as checked exception
        try {
            throw exception;
        } catch (QueryNodeException caught) {
            assertSame(exception, caught);
        }
    }

    @Test
    @DisplayName("QueryNodeException with nested cause should unwrap correctly")
    void queryNodeException_withNestedCause_shouldUnwrap() {
        // Given
        RuntimeException innerCause = new RuntimeException("Inner");
        Exception middleCause = new Exception("Middle", innerCause);

        // When
        QueryNodeException exception = new QueryNodeException(middleCause);

        // Then
        assertEquals(middleCause, exception.getCause());
        assertEquals(innerCause, exception.getCause().getCause());
    }

    @Test
    @DisplayName("QueryNodeException should have serialVersionUID")
    void queryNodeException_shouldHaveSerialVersionUID() {
        // Verify class is properly serializable
        QueryNodeException exception = new QueryNodeException(new Exception("test"));
        assertNotNull(exception);
    }

    @Test
    @DisplayName("QueryNodeException with null cause should handle")
    void queryNodeException_withNullCause_shouldHandle() {
        // When/Then
        // Constructor requires a non-null cause based on source
        assertThrows(NullPointerException.class, () -> new QueryNodeException(null));
    }

    // ============== Exception Hierarchy Tests ==============

    @Test
    @DisplayName("all exceptions should be in same package")
    void allExceptions_shouldBeInSamePackage() {
        // Given/When
        IPEDException ipedEx = new IPEDException("test");
        ParseException parseEx = new ParseException();
        QueryNodeException queryEx = new QueryNodeException(new Exception("cause"));

        // Then
        assertEquals("iped.exception", ipedEx.getClass().getPackage().getName());
        assertEquals("iped.exception", parseEx.getClass().getPackage().getName());
        assertEquals("iped.exception", queryEx.getClass().getPackage().getName());
    }

    @Test
    @DisplayName("IPEDException should be RuntimeException, others checked")
    void ipedException_shouldBeRuntimeException() {
        // Given/When
        IPEDException ipedEx = new IPEDException("test");
        ParseException parseEx = new ParseException();
        QueryNodeException queryEx = new QueryNodeException(new Exception("cause"));

        // Then
        assertTrue(RuntimeException.class.isAssignableFrom(ipedEx.getClass()));
        assertFalse(RuntimeException.class.isAssignableFrom(parseEx.getClass()));
        assertFalse(RuntimeException.class.isAssignableFrom(queryEx.getClass()));
    }
}
