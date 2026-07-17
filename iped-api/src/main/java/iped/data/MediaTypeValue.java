package iped.data;

import java.util.Objects;

/**
 * Neutral media type value for API contracts, wrapping the type's string representation (e.g.
 * {@code "image/png"}) without depending on Tika.
 */
public final class MediaTypeValue {

  private final String value;

  private MediaTypeValue(String value) {
    this.value = value;
  }

  /**
   * Creates a media type value from its string representation.
   *
   * @param value the media type string, e.g. {@code "application/pdf"}
   * @return a new instance wrapping the given value
   */
  public static MediaTypeValue of(String value) {
    return new MediaTypeValue(value);
  }

  /**
   * @return the wrapped media type string
   */
  public String value() {
    return value;
  }

  @Override
  public String toString() {
    return value;
  }

  @Override
  public int hashCode() {
    return Objects.hash(value);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (!(obj instanceof MediaTypeValue other)) {
      return false;
    }
    return Objects.equals(value, other.value);
  }
}
