package iped.webui.config;

import java.time.Duration;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.CacheControl;
import org.springframework.web.client.RestClient;
import org.springframework.context.annotation.Bean;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Static-asset handling and the {@link RestClient} used by the API proxy.
 *
 * <p>Island bundles are content-hashed by the Angular production build, so they
 * are served immutable with a long max-age for aggressive cache busting.
 */
@Configuration
@EnableConfigurationProperties(WebUiProperties.class)
public class WebConfig implements WebMvcConfigurer {

    private final WebUiProperties properties;

    public WebConfig(WebUiProperties properties) {
        this.properties = properties;
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/islands/**")
                .addResourceLocations("classpath:/static/islands/")
                .setCacheControl(CacheControl.maxAge(Duration.ofDays(365)).immutable());
    }

    @Bean
    RestClient ipedWebapiClient() {
        return RestClient.builder()
                .baseUrl(properties.apiBaseUrl())
                .build();
    }
}
