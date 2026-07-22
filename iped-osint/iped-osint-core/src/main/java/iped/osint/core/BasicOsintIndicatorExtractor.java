package iped.osint.core;

import iped.data.IItemReader;
import iped.osint.spi.OsintIndicatorType;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class BasicOsintIndicatorExtractor implements OsintIndicatorExtractor {

    private static final Pattern EMAIL = Pattern.compile("\\b[\\w.%+-]+@[\\w.-]+\\.[A-Za-z]{2,}\\b");
    private static final Pattern URL = Pattern.compile("\\bhttps?://[^\\s\"'<>]+", Pattern.CASE_INSENSITIVE);
    private static final Pattern DOMAIN = Pattern.compile("\\b(?:[A-Za-z0-9-]+\\.)+[A-Za-z]{2,}\\b");
    private static final Pattern IPV4 = Pattern.compile("\\b(?:\\d{1,3}\\.){3}\\d{1,3}\\b");
    private static final Pattern PHONE = Pattern.compile("\\+?[0-9][0-9()\\-\\s]{7,}[0-9]");
    private static final Pattern HASH = Pattern.compile("\\b(?:[A-Fa-f0-9]{32}|[A-Fa-f0-9]{40}|[A-Fa-f0-9]{64})\\b");
    private static final Pattern USERNAME = Pattern.compile("(?<![\\w@])@[A-Za-z0-9._-]{3,}");

    @Override
    public List<OsintIndicator> extract(IItemReader item) {
        Set<OsintIndicator> indicators = new LinkedHashSet<>();
        addMatches(indicators, EMAIL, OsintIndicatorType.EMAIL, item.getName());
        addMatches(indicators, URL, OsintIndicatorType.URL, item.getPath());
        addMatches(indicators, DOMAIN, OsintIndicatorType.DOMAIN, item.getPath());
        addMatches(indicators, HASH, OsintIndicatorType.HASH, item.getHash());
        addMatches(indicators, USERNAME, OsintIndicatorType.USERNAME, item.getName());

        for (Map.Entry<String, List<String>> entry : item.getMetadataMap().entrySet()) {
            for (String value : entry.getValue()) {
                addMatches(indicators, EMAIL, OsintIndicatorType.EMAIL, value);
                addMatches(indicators, URL, OsintIndicatorType.URL, value);
                addMatches(indicators, DOMAIN, OsintIndicatorType.DOMAIN, value);
                addMatches(indicators, IPV4, OsintIndicatorType.IP, value);
                addMatches(indicators, PHONE, OsintIndicatorType.PHONE, value);
                addMatches(indicators, HASH, OsintIndicatorType.HASH, value);
                addMatches(indicators, USERNAME, OsintIndicatorType.USERNAME, value);
            }
        }
        return new ArrayList<>(indicators);
    }

    private static void addMatches(Set<OsintIndicator> out, Pattern pattern, OsintIndicatorType type, String input) {
        if (input == null || input.isBlank()) {
            return;
        }
        Matcher matcher = pattern.matcher(input);
        while (matcher.find()) {
            String value = matcher.group();
            if (type == OsintIndicatorType.USERNAME && value.startsWith("@")) {
                value = value.substring(1);
            }
            out.add(new OsintIndicator(type, value));
        }
    }
}
