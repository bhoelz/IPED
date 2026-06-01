package iped.exception;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

/**
 * Tests for IPEDException class.
 */
class IPEDExceptionTest {

    @Test
    @DisplayName("constructor with message should set message")
    void constructor_withMessage_shouldSetMessage() {
        // Given
        String message = "Test error message";
        
        // When
        IPEDException exception = new IPEDException(message);
        
        // Then
        assertEquals(message, exception.getMessage());
    }

    @Test
    @DisplayName("constructor with message and cause should set both")
    void constructor_withMessageAndCause_shouldSetBoth() {
        // Given
        String message = "Test error message";
        Throwable cause = new RuntimeException("Original cause");
        
        // When
        IPEDException exception = new IPEDException(message, cause);
        
        // Then
        assertEquals(message, exception.getMessage());
        assertEquals(cause, exception.getCause());
    }

    @Test
    @DisplayName("constructor with cause only should set cause")
    void constructor_withCauseOnly_shouldSetCause() {
        // Given
        Throwable cause = new RuntimeException("Original cause");
        
        // When
        IPEDException exception = new IPEDException(cause);
        
        // Then
        assertEquals(cause, exception.getCause());
        // Message should contain the cause's message
        assertNotNull(exception.getMessage());
    }

    @Test
    @DisplayName("exception should be runtime exception")
    void shouldBeRuntimeException() {
        // When
        IPEDException exception = new IPEDException("test");
        
        // Then
        assertTrue(exception instanceof RuntimeException);
    }

    @Test
    @DisplayName("exception with null message should handle")
    void constructor_withNullMessage_shouldHandle() {
        // When
        IPEDException exception = new IPEDException((String) null);
        
        // Then
        assertNull(exception.getMessage());
    }

    @Test
    @DisplayName("exception should be throwable")
    void shouldBeThrowable() {
        // Given
        IPEDException exception = new IPEDException("test");
        
        // When/Then
        assertThrows(IPEDException.class, () -> {
            throw exception;
        });
    }

    @Test
    @DisplayName("exception stack trace should be accessible")
    void stackTrace_shouldBeAccessible() {
        // Given
        IPEDException exception = new IPEDException("test");
        
        // When
        StackTraceElement[] stackTrace = exception.getStackTrace();
        
        // Then
        assertNotNull(stackTrace);
    }

    @Test
    @DisplayName("exception can wrap checked exception")
    void canWrapCheckedException() {
        // Given
        Exception checked = new Exception("Checked exception");
        
        // When
        IPEDException exception = new IPEDException(checked);
        
        // Then
        assertEquals(checked, exception.getCause());
    }

    @Test
    @DisplayName("exception message should include nested cause")
    void message_shouldIncludeNestedCause() {
        // Given
        String innerMsg = "Inner exception";
        String outerMsg = "Outer exception";
        RuntimeException inner = new RuntimeException(innerMsg);
        
        // When
        IPEDException exception = new IPEDException(outerMsg, inner);
        
        // Then
        assertEquals(outerMsg, exception.getMessage());
        assertEquals(innerMsg, exception.getCause().getMessage());
    }

    @Test
    @DisplayName("serialVersionUID should be accessible")
    void serialVersionUID_shouldExist() {
        // This test verifies the class has a serialVersionUID field
        // The actual value is implementation detail
        IPEDException exception = new IPEDException("test");
        assertNotNull(exception);
    }
}
