package iped.parsers.browsers.chrome;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class ChromeCacheExceptionTest {

  @Test
  void constructor_withMessage_thenGetMessageReturns() {
    ChromeCacheException ex = new ChromeCacheException("test error");
    assertEquals("test error", ex.getMessage());
  }

  @Test
  void extendsException() {
    ChromeCacheException ex = new ChromeCacheException("error");
    assertInstanceOf(Exception.class, ex);
  }

  @Test
  void canBeThrownAndCaught() {
    assertThrows(
        ChromeCacheException.class,
        () -> {
          throw new ChromeCacheException("thrown");
        });
  }
}
