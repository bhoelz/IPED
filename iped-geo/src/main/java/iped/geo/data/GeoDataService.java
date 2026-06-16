package iped.geo.data;

import iped.data.IIPEDSource;
import iped.data.IItem;
import iped.data.IItemId;
import iped.properties.ExtraProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Default implementation of {@link IGeoDataService}.
 *
 * <p>Reads {@code ExtraProperties.LOCATIONS} values ({@code "lat;lon"} pairs) from
 * the item metadata map (via the reflective {@code IItem.getMetadataMap()} API so
 * that no compile dependency on Tika is required from this layer). Altitude is read
 * from {@code ExtraProperties.COMMON_META_PREFIX + "altitude"}.
 *
 * <p><strong>UI-toolkit free:</strong> this class imports no {@code java.awt},
 * {@code javax.swing}, or {@code javafx} types and can be loaded in a headless JVM.
 */
public class GeoDataService implements IGeoDataService {

    private static final Logger log = LoggerFactory.getLogger(GeoDataService.class);

    private static final String ALTITUDE_KEY =
            ExtraProperties.COMMON_META_PREFIX + "altitude";

    @Override
    public List<GeoFeature> extractFeatures(IIPEDSource source,
                                            Iterable<? extends IItemId> itemIds) {
        List<GeoFeature> features = new ArrayList<>();
        for (IItemId itemId : itemIds) {
            IItem item = source.getItemByID(itemId.getId());
            if (item == null) continue;
            try {
                features.addAll(featuresFromItem(itemId.getSourceId(), itemId.getId(), item));
            } catch (Exception e) {
                log.warn("Skipping geo extraction for item {}:{} — {}", itemId.getSourceId(),
                        itemId.getId(), e.getMessage());
            }
        }
        return features;
    }

    private static List<GeoFeature> featuresFromItem(int sourceId, int docId, IItem item) {
        Map<String, List<String>> meta = item.getMetadataMap();
        List<String> locations = meta.getOrDefault(ExtraProperties.LOCATIONS, List.of());
        if (locations.isEmpty()) return List.of();

        String name = item.getName() != null ? item.getName() : "";
        // "dcterms:created" is the Tika key for creation/capture timestamp.
        String timestamp = firstValue(meta, "dcterms:created");
        Double altitude  = parseDouble(firstValue(meta, ALTITUDE_KEY));

        List<GeoFeature> result = new ArrayList<>(locations.size());
        for (String loc : locations) {
            String[] parts = loc.split(";");
            if (parts.length < 2) continue;
            try {
                double lat = Double.parseDouble(parts[0].trim());
                double lon = Double.parseDouble(parts[1].trim());
                result.add(new GeoFeature(sourceId, docId, name, lat, lon, altitude, timestamp));
            } catch (NumberFormatException e) {
                log.debug("Unparseable location '{}' on item {}:{}", loc, sourceId, docId);
            }
        }
        return result;
    }

    private static String firstValue(Map<String, List<String>> meta, String key) {
        List<String> vals = meta.get(key);
        return (vals != null && !vals.isEmpty()) ? vals.get(0) : null;
    }

    private static Double parseDouble(String s) {
        if (s == null || s.isBlank()) return null;
        try {
            return Double.parseDouble(s.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
