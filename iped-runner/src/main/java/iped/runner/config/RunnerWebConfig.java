package iped.runner.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.http.CacheControl;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.time.Duration;

/**
 * Static-asset caching for the Runner UI.
 *
 * <p>Runner JS/CSS files are versioned via the Maven project version so they
 * get a long-lived cache; vendor assets (htmx) are served immutable.
 */
@Configuration
public class RunnerWebConfig implements WebMvcConfigurer {

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/runner/**")
                .addResourceLocations("classpath:/static/runner/")
                .setCacheControl(CacheControl.maxAge(Duration.ofDays(7)));

        registry.addResourceHandler("/vendor/**")
                .addResourceLocations("classpath:/static/vendor/")
                .setCacheControl(CacheControl.maxAge(Duration.ofDays(365)).immutable());
    }
}
