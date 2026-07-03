package iped.osint.core;

import iped.osint.spi.OsintContext;
import iped.osint.spi.OsintExecutionMode;
import iped.osint.spi.OsintHit;
import iped.osint.spi.OsintIndicatorType;
import iped.osint.spi.OsintPluginDescriptor;
import iped.osint.spi.OsintPluginException;
import iped.osint.spi.OsintPluginProvider;
import iped.osint.spi.OsintQuery;
import iped.osint.spi.OsintResult;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.net.http.HttpClient;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

@Slf4j
public class OsintService {

    private final String caseId;
    private final OsintPluginRegistry registry;
    private final OsintResultStore store;
    private final OsintPluginsConfig config;
    private final OsintPolicyEngine policyEngine;
    private final OsintRateLimiter rateLimiter;
    private final Clock clock;
    private final HttpClient httpClient;

    public OsintService(String caseId, OsintPluginRegistry registry, OsintResultStore store,
                        OsintPluginsConfig config, Clock clock) {
        this.caseId = caseId;
        this.registry = registry;
        this.store = store;
        this.config = config;
        this.policyEngine = new OsintPolicyEngine(config);
        this.clock = clock == null ? Clock.systemUTC() : clock;
        this.rateLimiter = new OsintRateLimiter(this.clock);
        this.httpClient = HttpClient.newBuilder().connectTimeout(java.time.Duration.ofSeconds(10)).build();
    }

    public List<OsintPluginDescriptor> listPlugins() {
        return registry.descriptors().stream()
                .sorted(Comparator.comparing(OsintPluginDescriptor::id))
                .toList();
    }

    public Optional<OsintPluginDescriptor> getPlugin(String pluginId) {
        return registry.provider(pluginId).map(OsintPluginProvider::descriptor);
    }

    public OsintResult execute(String pluginId, OsintIndicatorType indicatorType, String value, Integer itemId,
                               String sourceId, OsintExecutionMode mode, Map<String, Object> options)
            throws OsintPluginException, IOException {
        OsintPluginProvider provider = registry.provider(pluginId)
                .orElseThrow(() -> new OsintPluginException("Unknown OSINT plugin: " + pluginId));
        OsintPluginDescriptor descriptor = provider.descriptor();
        var decision = policyEngine.evaluate(descriptor, mode);
        if (!decision.allowed()) {
            throw new OsintPluginException("OSINT policy denied execution: " + decision.reason());
        }

        if (!rateLimiter.tryAcquire(pluginId, config.rateLimitPerMinute(pluginId, 120))) {
            throw new OsintPluginException("OSINT rate limit exceeded for plugin " + pluginId);
        }

        String normalized = provider.normalize(indicatorType, value);
        String fingerprint = fingerprint(pluginId, indicatorType, normalized, itemId, sourceId, options);
        if (!Boolean.TRUE.equals(options.get("refresh"))) {
            Optional<OsintResult> cached = store.findLatestByFingerprint(pluginId, itemId, fingerprint);
            if (cached.isPresent()) {
                return cached.get();
            }
        }

        Instant startedAt = clock.instant();
        String executionId = UUID.randomUUID().toString();
        OsintQuery query = new OsintQuery(pluginId, indicatorType, value, normalized, itemId, sourceId, mode, options);
        OsintContext context = new OsintContext(caseId, sourceId, itemId, mode, config.credentials(pluginId), options,
                httpClient, clock);
        int timeoutSeconds = config.timeoutSeconds(pluginId, descriptor.defaultTimeoutSeconds());
        Future<OsintResult> future = Executors.newSingleThreadExecutor().submit(() -> provider.execute(query, context));

        try {
            OsintResult raw = future.get(timeoutSeconds, TimeUnit.SECONDS);
            OsintResult result = new OsintResult(
                    executionId,
                    pluginId,
                    indicatorType,
                    value,
                    normalized,
                    fingerprint,
                    raw.status() == null ? "completed" : raw.status(),
                    startedAt,
                    clock.instant(),
                    itemId,
                    sourceId,
                    raw.hits(),
                    mergeAudit(raw.audit(), descriptor, mode, timeoutSeconds));
            if (config.persistResults()) {
                store.save(result);
            }
            return result;
        } catch (ExecutionException e) {
            Throwable cause = e.getCause();
            if (cause instanceof OsintPluginException pluginException) {
                throw pluginException;
            }
            throw new OsintPluginException("OSINT execution failed for plugin " + pluginId, cause);
        } catch (Exception e) {
            future.cancel(true);
            throw new OsintPluginException("OSINT execution timed out or failed for plugin " + pluginId, e);
        }
    }

    public List<OsintResult> listResults(String sourceId, Integer itemId, String pluginId, int limit) throws IOException {
        return store.list(sourceId, itemId, pluginId, limit);
    }

    public Optional<OsintResult> getResult(String executionId) throws IOException {
        return store.get(executionId);
    }

    public List<OsintResult> searchItem(int itemId, String sourceId, OsintExecutionMode mode,
                                        List<String> pluginIds, Map<String, Object> options,
                                        iped.data.IItemReader item, OsintIndicatorExtractor extractor)
            throws IOException {
        List<OsintIndicator> indicators = extractor.extract(item);
        List<OsintResult> results = new ArrayList<>();
        for (OsintIndicator indicator : indicators) {
            for (String pluginId : resolvePlugins(pluginIds, indicator.type())) {
                try {
                    results.add(execute(pluginId, indicator.type(), indicator.value(), itemId, sourceId, mode, options));
                } catch (OsintPluginException e) {
                    log.warn("OSINT plugin {} failed for item {}: {}", pluginId, itemId, e.getMessage());
                }
            }
        }
        return results;
    }

    public List<String> resolvePlugins(List<String> requestedPluginIds, OsintIndicatorType indicatorType) {
        if (requestedPluginIds != null && !requestedPluginIds.isEmpty()) {
            return requestedPluginIds;
        }
        List<String> resolved = new ArrayList<>();
        for (OsintPluginDescriptor descriptor : listPlugins()) {
            if (descriptor.supportedIndicatorTypes().contains(indicatorType)) {
                resolved.add(descriptor.id());
            }
        }
        return resolved;
    }

    public Map<String, Object> summarize(List<OsintResult> results) {
        List<String> executionIds = results.stream().map(OsintResult::executionId).toList();
        List<String> plugins = results.stream().map(OsintResult::pluginId).distinct().sorted().toList();
        int hitCount = results.stream().map(OsintResult::hits).mapToInt(List::size).sum();
        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("osint.executionIds", executionIds);
        summary.put("osint.plugins", plugins);
        summary.put("osint.hitCount", hitCount);
        summary.put("osint.lastExecutionAt", results.stream().map(OsintResult::finishedAt).max(Comparator.naturalOrder()).orElse(null));
        return summary;
    }

    public List<Map<String, Object>> flattenHits(List<OsintResult> results) {
        List<Map<String, Object>> flattened = new ArrayList<>();
        for (OsintResult result : results) {
            for (OsintHit hit : result.hits()) {
                flattened.add(Map.of(
                        "executionId", result.executionId(),
                        "pluginId", result.pluginId(),
                        "indicatorType", result.indicatorType().name(),
                        "indicator", result.normalizedValue(),
                        "source", hit.source(),
                        "title", hit.title(),
                        "summary", hit.summary(),
                        "confidence", hit.confidence()));
            }
        }
        return flattened;
    }

    private Map<String, Object> mergeAudit(Map<String, Object> rawAudit, OsintPluginDescriptor descriptor,
                                           OsintExecutionMode mode, int timeoutSeconds) {
        Map<String, Object> audit = new LinkedHashMap<>(rawAudit == null ? Map.of() : rawAudit);
        audit.put("caseId", caseId);
        audit.put("mode", mode.name());
        audit.put("pluginDisplayName", descriptor.displayName());
        audit.put("timeoutSeconds", timeoutSeconds);
        return audit;
    }

    private static String fingerprint(String pluginId, OsintIndicatorType type, String normalizedValue, Integer itemId,
                                      String sourceId, Map<String, Object> options) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            String material = pluginId + "|" + type + "|" + normalizedValue + "|" + itemId + "|" + sourceId + "|" + options;
            return HexFormat.of().formatHex(digest.digest(material.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new IllegalStateException("Could not compute OSINT fingerprint", e);
        }
    }
}
