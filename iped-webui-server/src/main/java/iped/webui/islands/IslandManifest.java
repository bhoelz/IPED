package iped.webui.islands;

import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Resolves the content-hashed Angular island bundle filenames produced by the
 * production build (e.g. {@code main-AB12CD34.js}) so templates can reference
 * them without knowing the hash.
 *
 * <p>The Angular {@code islands} build stages its output under
 * {@code classpath:/static/islands/}; this bean scans that location once at
 * startup. When no bundle is present (e.g. a dev run where the frontend build
 * was skipped) the lists are empty and the SSR page still renders — the island
 * host element simply never upgrades.
 */
@Component
@Slf4j
public class IslandManifest {

    private static final String BASE = "/islands/";

    private final List<String> scripts;
    private final List<String> styles;

    public IslandManifest() {
        this.scripts = resolve("classpath:/static/islands/*.js");
        this.styles = resolve("classpath:/static/islands/*.css");
        if (scripts.isEmpty()) {
            log.warn("No Angular island bundles found under static/islands/. "
                    + "Run the frontend build (npm run build:islands) so islands can mount.");
        } else {
            log.info("Resolved island bundles: scripts={}, styles={}", scripts, styles);
        }
    }

    /** ES module script URLs, ordered so polyfills load before the entry. */
    public List<String> scripts() {
        return scripts;
    }

    /** Stylesheet URLs for the islands. */
    public List<String> styles() {
        return styles;
    }

    private static List<String> resolve(String pattern) {
        var resolver = new PathMatchingResourcePatternResolver();
        var result = new ArrayList<String>();
        try {
            Resource[] resources = resolver.getResources(pattern);
            for (Resource r : resources) {
                String name = r.getFilename();
                if (name != null) {
                    result.add(BASE + name);
                }
            }
        } catch (IOException e) {
            log.warn("Failed to scan island bundles for pattern {}: {}", pattern, e.getMessage());
        }
        // polyfills before main before anything else; otherwise stable order
        result.sort(Comparator.comparingInt(IslandManifest::loadRank).thenComparing(Comparator.naturalOrder()));
        return List.copyOf(result);
    }

    private static int loadRank(String path) {
        if (path.contains("/polyfills")) {
            return 0;
        }
        if (path.contains("/main")) {
            return 1;
        }
        return 2;
    }
}
