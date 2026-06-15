package iped.webui.islands;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.*;

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
 *
 * <p>If a {@code manifest.json} (written by {@code generate-islands-manifest.mjs})
 * is present, per-island chunk URLs are available via {@link #chunkFor(String)}
 * for targeted {@code <link rel="modulepreload">} injection.
 */
@Component
@Slf4j
public class IslandManifest {

    private static final String BASE = "/islands/";

    private final List<String> scripts;
    private final List<String> styles;

    /** island-name → hashed chunk URL, populated from manifest.json when available. */
    private final Map<String, String> chunkByIsland;

    public IslandManifest() {
        this.scripts = resolve("classpath:/static/islands/*.js");
        this.styles  = resolve("classpath:/static/islands/*.css");
        this.chunkByIsland = loadManifestChunks();

        if (scripts.isEmpty()) {
            log.warn("No Angular island bundles found under static/islands/. "
                    + "Run the frontend build (npm run build:islands) so islands can mount.");
        } else {
            log.info("Resolved island bundles: scripts={}, styles={}, manifest-chunks={}",
                    scripts, styles, chunkByIsland.keySet());
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

    /**
     * Returns the hashed chunk URL for the named island, if the manifest is present.
     * Use to emit {@code <link rel="modulepreload" href="...">} only for the islands
     * needed on a given page.
     *
     * @param islandName one of {@code results-grid, gallery, hex-viewer, timeline, graph}
     */
    public Optional<String> chunkFor(String islandName) {
        return Optional.ofNullable(chunkByIsland.get(islandName));
    }

    /** All chunk mappings known from the manifest, or empty when manifest is absent. */
    public Map<String, String> allChunks() {
        return chunkByIsland;
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
        // polyfills before main before chunk files
        result.sort(Comparator.comparingInt(IslandManifest::loadRank).thenComparing(Comparator.naturalOrder()));
        return List.copyOf(result);
    }

    @SuppressWarnings("unchecked")
    private static Map<String, String> loadManifestChunks() {
        var resource = new ClassPathResource("static/islands/manifest.json");
        if (!resource.exists()) {
            return Map.of();
        }
        try {
            var raw = new ObjectMapper().readValue(resource.getInputStream(),
                    new TypeReference<Map<String, Object>>() {});
            Object chunks = raw.get("chunks");
            if (chunks instanceof Map<?, ?> cm) {
                Map<String, String> result = new LinkedHashMap<>();
                cm.forEach((k, v) -> {
                    if (k != null && v != null) result.put(k.toString(), v.toString());
                });
                return Collections.unmodifiableMap(result);
            }
        } catch (IOException e) {
            log.warn("Could not parse islands manifest.json: {}", e.getMessage());
        }
        return Map.of();
    }

    private static int loadRank(String path) {
        if (path.contains("/polyfills")) return 0;
        if (path.contains("/main"))     return 1;
        return 2;
    }
}
