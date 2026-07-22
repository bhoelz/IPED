package iped.osint.plugins.basic;

import iped.osint.spi.OsintCapability;
import iped.osint.spi.OsintContext;
import iped.osint.spi.OsintEvidenceRef;
import iped.osint.spi.OsintExecutionMode;
import iped.osint.spi.OsintHit;
import iped.osint.spi.OsintIndicatorType;
import iped.osint.spi.OsintPluginDescriptor;
import iped.osint.spi.OsintPluginException;
import iped.osint.spi.OsintPluginProvider;
import iped.osint.spi.OsintQuery;
import iped.osint.spi.OsintResult;

import java.time.Instant;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class BasicProfileLookupPlugin implements OsintPluginProvider {

    @Override
    public OsintPluginDescriptor descriptor() {
        return new OsintPluginDescriptor(
                "basic-profile-lookup",
                "Basic Profile Lookup",
                Set.of(OsintIndicatorType.USERNAME, OsintIndicatorType.EMAIL, OsintIndicatorType.DOMAIN, OsintIndicatorType.URL),
                EnumSet.of(OsintExecutionMode.PROCESSING, OsintExecutionMode.ANALYSIS, OsintExecutionMode.SCRIPT, OsintExecutionMode.MCP),
                Set.of(OsintCapability.NETWORK),
                "Passive first-party plugin that derives common public profile and infrastructure lookups.",
                Map.of("kind", "derived-links"),
                10,
                1);
    }

    @Override
    public String normalize(OsintIndicatorType indicatorType, String value) {
        return value == null ? null : value.trim().toLowerCase();
    }

    @Override
    public OsintResult execute(OsintQuery query, OsintContext context) throws OsintPluginException {
        List<OsintHit> hits = new ArrayList<>();
        switch (query.indicatorType()) {
            case USERNAME -> {
                hits.add(hit(query, "github", "https://github.com/" + query.normalizedValue()));
                hits.add(hit(query, "reddit", "https://www.reddit.com/user/" + query.normalizedValue()));
            }
            case EMAIL -> hits.add(hit(query, "gravatar", "https://gravatar.com/" + query.normalizedValue()));
            case DOMAIN -> {
                hits.add(hit(query, "crt.sh", "https://crt.sh/?q=" + query.normalizedValue()));
                hits.add(hit(query, "urlscan", "https://urlscan.io/domain/" + query.normalizedValue()));
            }
            case URL -> hits.add(hit(query, "urlscan", "https://urlscan.io/search/#" + query.normalizedValue()));
            default -> {
            }
        }
        Instant now = context.clock().instant();
        return new OsintResult(
                null,
                descriptor().id(),
                query.indicatorType(),
                query.value(),
                query.normalizedValue(),
                null,
                "completed",
                now,
                now,
                query.itemId(),
                query.sourceId(),
                hits,
                Map.of("provider", getClass().getName(), "hitCount", hits.size()));
    }

    private OsintHit hit(OsintQuery query, String source, String url) {
        return new OsintHit(
                descriptor().id(),
                source,
                source + " lookup for " + query.normalizedValue(),
                "Derived passive lookup endpoint for " + query.indicatorType().name().toLowerCase(),
                0.5d,
                List.of(new OsintEvidenceRef(source, url, query.normalizedValue(), Map.of("url", url))),
                Map.of("url", url));
    }
}
