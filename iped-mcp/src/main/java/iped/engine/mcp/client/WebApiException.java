package iped.engine.mcp.client;

public class WebApiException extends Exception {
  public WebApiException(String message) {
    super(message);
  }

  public WebApiException(String message, Throwable cause) {
    super(message, cause);
  }
}
