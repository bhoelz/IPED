package iped.exception;

import java.io.Serial;

/**
 * Generic unchecked exception for IPED errors that should abort the current operation and be
 * reported to the user without a stack-trace-worthy cause.
 */
public class IPEDException extends RuntimeException {

  @Serial private static final long serialVersionUID = 1L;

  /**
   * Creates an exception with the given message.
   *
   * @param msg the detail message
   */
  public IPEDException(String msg) {
    super(msg);
  }

  /**
   * Creates an exception with the given message and cause.
   *
   * @param msg the detail message
   * @param e the underlying cause
   */
  public IPEDException(String msg, Throwable e) {
    super(msg, e);
  }

  /**
   * Creates an exception wrapping the given cause.
   *
   * @param e the underlying cause
   */
  public IPEDException(Throwable e) {
    super(e);
  }
}
