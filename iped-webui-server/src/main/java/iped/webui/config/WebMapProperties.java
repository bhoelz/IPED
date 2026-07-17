package iped.webui.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Tile-source configuration for the {@code <iped-map>} island.
 *
 * <p>Defaults to the OpenStreetMap public tile server. For air-gapped labs, set {@code
 * iped.webui.map.tile-url} to a local tile server URL (e.g. a self-hosted TileServer-GL or MapProxy
 * instance).
 *
 * <p>Example in {@code application.yml}:
 *
 * <pre>
 * iped:
 *   webui:
 *     map:
 *       tile-url: "http://tiles.local/{z}/{x}/{y}.png"
 *       tile-attribution: "&copy; Local Tile Mirror"
 *       height: "420px"
 * </pre>
 */
@Component
@ConfigurationProperties(prefix = "iped.webui.map")
public class WebMapProperties {

  private String tileUrl = "https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png";
  private String tileAttribution =
      "&copy; <a href=\"https://www.openstreetmap.org/copyright\">OpenStreetMap</a> contributors";
  private String height = "420px";

  public String getTileUrl() {
    return tileUrl;
  }

  public void setTileUrl(String tileUrl) {
    this.tileUrl = tileUrl;
  }

  public String getTileAttribution() {
    return tileAttribution;
  }

  public void setTileAttribution(String tileAttribution) {
    this.tileAttribution = tileAttribution;
  }

  public String getHeight() {
    return height;
  }

  public void setHeight(String height) {
    this.height = height;
  }
}
