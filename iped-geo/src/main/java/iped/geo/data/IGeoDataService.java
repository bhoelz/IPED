package iped.geo.data;

import iped.data.IIPEDSource;
import iped.data.IItemId;

import java.util.List;

/**
 * Headless geo-data extraction service.
 *
 * <p>Implementations must not reference any UI toolkit ({@code javafx},
 * {@code java.awt}, {@code javax.swing}) so that this interface can be used from
 * headless server contexts such as {@code iped-webapi}.
 *
 * <p>The default implementation is {@link GeoDataService}.
 */
public interface IGeoDataService {

    /**
     * Extracts all geolocation features from the given items in the given source.
     *
     * <p>Items that carry no location metadata contribute zero features. Items that
     * carry multiple location values (e.g. GPX tracks, multi-waypoint KML) contribute
     * one {@link GeoFeature} per location.
     *
     * @param source  the open IPED case source
     * @param itemIds the items to inspect; may be empty
     * @return ordered list of features; never {@code null}
     */
    List<GeoFeature> extractFeatures(IIPEDSource source, Iterable<? extends IItemId> itemIds);
}
