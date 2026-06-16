package iped.app.companion;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

/**
 * Default {@link CompanionHandoff} implementation that POSTs to the companion
 * app's loopback HTTP server.
 *
 * <p>The companion listens on {@code http://127.0.0.1:<port>/open} (default port
 * {@value #DEFAULT_PORT}). Authentication uses an HMAC-SHA256 session token
 * issued by the companion at startup.
 */
public class HttpCompanionHandoff implements CompanionHandoff {

    public static final int DEFAULT_PORT = 18743;

    private final int port;
    private final String sessionToken;
    private final HttpClient http;

    /** Cache to avoid a TCP probe on every call. */
    private volatile long lastAvailableCheckMs = -1;
    private volatile boolean lastAvailableResult = false;
    private static final long AVAILABILITY_CACHE_MS = 1_000;

    public HttpCompanionHandoff(int port, String sessionToken) {
        this.port = port;
        this.sessionToken = sessionToken;
        this.http = HttpClient.newBuilder()
                .connectTimeout(Duration.ofMillis(500))
                .build();
    }

    public HttpCompanionHandoff(String sessionToken) {
        this(DEFAULT_PORT, sessionToken);
    }

    @Override
    public boolean isAvailable() {
        long now = System.currentTimeMillis();
        if (now - lastAvailableCheckMs < AVAILABILITY_CACHE_MS) {
            return lastAvailableResult;
        }
        try {
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create("http://127.0.0.1:" + port + "/version"))
                    .timeout(Duration.ofMillis(400))
                    .GET()
                    .build();
            HttpResponse<Void> resp = http.send(req, HttpResponse.BodyHandlers.discarding());
            lastAvailableResult = resp.statusCode() == 200;
        } catch (Exception e) {
            lastAvailableResult = false;
        }
        lastAvailableCheckMs = System.currentTimeMillis();
        return lastAvailableResult;
    }

    @Override
    public boolean openItem(CompanionHandoffRequest request) throws IOException {
        String body = buildJson(request);
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create("http://127.0.0.1:" + port + "/open"))
                .timeout(Duration.ofSeconds(5))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + sessionToken)
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();
        try {
            HttpResponse<Void> resp = http.send(req, HttpResponse.BodyHandlers.discarding());
            return resp.statusCode() == 200;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        }
    }

    @Override
    public int companionProtocolVersion() {
        if (!isAvailable()) return -1;
        try {
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create("http://127.0.0.1:" + port + "/version"))
                    .timeout(Duration.ofSeconds(2))
                    .GET()
                    .build();
            HttpResponse<String> resp = http.send(req, HttpResponse.BodyHandlers.ofString());
            if (resp.statusCode() != 200) return -1;
            String body = resp.body();
            int idx = body.indexOf("\"protocol\":");
            if (idx < 0) return 1;
            int start = idx + 11;
            int end = start;
            while (end < body.length() && Character.isDigit(body.charAt(end))) end++;
            return Integer.parseInt(body.substring(start, end));
        } catch (Exception e) {
            return -1;
        }
    }

    private static String buildJson(CompanionHandoffRequest r) {
        return "{" +
                "\"sourceId\":" + quote(r.sourceId()) + "," +
                "\"docId\":" + r.docId() + "," +
                "\"mimeType\":" + quote(r.mimeType()) + "," +
                "\"itemName\":" + quote(r.itemName()) + "," +
                "\"contentUrl\":" + quote(r.contentUrl()) + "," +
                "\"apiKey\":" + quote(r.apiKey()) +
                "}";
    }

    private static String quote(String s) {
        if (s == null) return "null";
        return "\"" + s.replace("\\", "\\\\").replace("\"", "\\\"") + "\"";
    }
}
