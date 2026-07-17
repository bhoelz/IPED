package iped.engine.mcp;

import java.time.Instant;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Fixed-window rate limiter for MCP tool invocations.
 *
 * <p>Counts calls in one-minute windows. When the window limit is exhausted the caller receives a
 * {@link RateLimitException}. The window resets automatically on the next call after the minute
 * elapses.
 */
public final class ToolRateLimiter {

  public static final class RateLimitException extends RuntimeException {
    public RateLimitException(int limit) {
      super(
          "Rate limit exceeded: "
              + limit
              + " tool calls per minute allowed. "
              + "Slow down and try again in the next window.");
    }
  }

  private final int maxPerMinute;
  private final AtomicLong windowStart = new AtomicLong(Instant.now().toEpochMilli());
  private final AtomicInteger count = new AtomicInteger(0);

  public ToolRateLimiter(int maxPerMinute) {
    this.maxPerMinute = maxPerMinute;
  }

  /**
   * Records one tool invocation. Throws {@link RateLimitException} when the per-minute cap is
   * exceeded.
   */
  public void acquire() {
    long now = Instant.now().toEpochMilli();
    long winStart = windowStart.get();
    if (now - winStart >= 60_000L) {
      // New window: reset count. Two threads may race here; the second
      // compareAndSet loses and falls through to the count check — that's
      // fine: worst case a few calls slip through at the boundary.
      if (windowStart.compareAndSet(winStart, now)) {
        count.set(0);
      }
    }
    int calls = count.incrementAndGet();
    if (calls > maxPerMinute) {
      throw new RateLimitException(maxPerMinute);
    }
  }

  /** Current call count in the active window (for tests / observability). */
  public int currentCount() {
    return count.get();
  }
}
