package iped.geo.data;

/**
 * Immutable value type representing a single geolocation extracted from an item.
 *
 * <p>One item may contribute multiple {@code GeoFeature} instances (e.g. a GPX track or a
 * burst-photo set). All coordinates follow the GeoJSON convention: longitude first, latitude
 * second.
 *
 * <p>This type is part of the <em>headless geo data layer</em> ({@code iped.geo.data}) — it must
 * remain free of any {@code java.awt}, {@code javax.swing}, or {@code javafx} imports so it can be
 * consumed from {@code iped-webapi} without pulling in a UI toolkit.
 *
 * @param sourceId numeric source identifier (matches {@code IItemId.getSourceId()})
 * @param docId document identifier within the source (matches {@code IItemId.getId()})
 * @param name item name (file name), never {@code null}
 * @param latitude WGS-84 latitude in decimal degrees
 * @param longitude WGS-84 longitude in decimal degrees
 * @param altitude altitude in metres above sea level, or {@code null} if unavailable
 * @param timestamp ISO-8601 creation / capture timestamp, or {@code null} if unavailable
 */
public record GeoFeature(
    int sourceId,
    int docId,
    String name,
    double latitude,
    double longitude,
    Double altitude,
    String timestamp) {}
