package iped.engine.mcp;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class ToolRateLimiterTest {

  @Test
  void allowsCallsUpToLimit() {
    ToolRateLimiter limiter = new ToolRateLimiter(5);
    for (int i = 0; i < 5; i++) {
      assertDoesNotThrow(limiter::acquire);
    }
  }

  @Test
  void throwsOnExceedingLimit() {
    ToolRateLimiter limiter = new ToolRateLimiter(3);
    for (int i = 0; i < 3; i++) limiter.acquire();
    assertThrows(ToolRateLimiter.RateLimitException.class, limiter::acquire);
  }

  @Test
  void exceptionMessageContainsLimit() {
    ToolRateLimiter limiter = new ToolRateLimiter(2);
    limiter.acquire();
    limiter.acquire();
    var ex = assertThrows(ToolRateLimiter.RateLimitException.class, limiter::acquire);
    assertTrue(ex.getMessage().contains("2"), "should mention the limit");
  }

  @Test
  void countsCallsCorrectly() {
    ToolRateLimiter limiter = new ToolRateLimiter(100);
    limiter.acquire();
    limiter.acquire();
    assertEquals(2, limiter.currentCount());
  }
}
