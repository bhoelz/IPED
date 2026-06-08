package iped.webui.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration for the web UI server.
 *
 * @param apiBaseUrl base URL of the backing Jersey {@code iped-webapi} that
 *                   {@code /api/**} requests are proxied to.
 */
@ConfigurationProperties(prefix = "iped.webui")
public record WebUiProperties(String apiBaseUrl) {

    public WebUiProperties {
        if (apiBaseUrl == null || apiBaseUrl.isBlank()) {
            apiBaseUrl = "http://localhost:8080";
        }
        // normalise: no trailing slash, so path joining is predictable
        while (apiBaseUrl.endsWith("/")) {
            apiBaseUrl = apiBaseUrl.substring(0, apiBaseUrl.length() - 1);
        }
    }
}
