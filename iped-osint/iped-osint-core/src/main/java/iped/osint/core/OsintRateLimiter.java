package iped.osint.core;

import java.time.Clock;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class OsintRateLimiter {

    private final Clock clock;
    private final Map<String, Window> windows = new ConcurrentHashMap<>();

    public OsintRateLimiter(Clock clock) {
        this.clock = clock;
    }

    public synchronized boolean tryAcquire(String pluginId, int limitPerMinute) {
        if (limitPerMinute <= 0) {
            return true;
        }
        Instant now = clock.instant();
        Window window = windows.computeIfAbsent(pluginId, ignored -> new Window(now, 0));
        if (now.isAfter(window.startedAt.plusSeconds(60))) {
            window.startedAt = now;
            window.count = 0;
        }
        if (window.count >= limitPerMinute) {
            return false;
        }
        window.count++;
        return true;
    }

    private static final class Window {
        private Instant startedAt;
        private int count;

        private Window(Instant startedAt, int count) {
            this.startedAt = startedAt;
            this.count = count;
        }
    }
}
