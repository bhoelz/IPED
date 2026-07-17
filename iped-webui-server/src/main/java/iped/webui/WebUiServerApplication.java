package iped.webui;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Composition root for the IPED web UI.
 *
 * <p>Spring Boot owns navigational routes, page layout, server-rendered Rocker templates and HTMX
 * fragment endpoints, and re-exposes {@code /api/**} by proxying to the Jersey {@code iped-webapi}.
 * Angular survives only as embedded custom-element "islands" mounted inside SSR pages.
 */
@SpringBootApplication
public class WebUiServerApplication {

  public static void main(String[] args) {
    SpringApplication.run(WebUiServerApplication.class, args);
  }
}
