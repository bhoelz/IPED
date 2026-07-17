package iped.exception;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class ExceptionsTest {

  @Test
  void ipedExceptionConstructors_whenCalled_thenPreserveMessageAndCause() {
    RuntimeException cause = new RuntimeException("cause");
    IPEDException withMessage = new IPEDException("msg");
    IPEDException withMessageAndCause = new IPEDException("msg2", cause);
    IPEDException withCause = new IPEDException(cause);

    assertEquals("msg", withMessage.getMessage());
    assertEquals("msg2", withMessageAndCause.getMessage());
    assertSame(cause, withMessageAndCause.getCause());
    assertSame(cause, withCause.getCause());
  }

  @Test
  void parseException_whenCreated_thenIsException() {
    ParseException ex = new ParseException();
    assertInstanceOf(Exception.class, ex);
  }

  @Test
  void queryNodeException_whenCreatedWithCause_thenPreservesCause() {
    Exception cause = new Exception("q");
    QueryNodeException ex = new QueryNodeException(cause);

    assertSame(cause, ex.getCause());
  }
}
