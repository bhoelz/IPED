package iped.data;

import java.util.Objects;

/**
 * Neutral media type value for API contracts.
 */
public final class MediaTypeValue {

    private final String value;

    private MediaTypeValue(String value) {
        this.value = value;
    }

    public static MediaTypeValue of(String value) {
        return new MediaTypeValue(value);
    }

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
        if (!(obj instanceof MediaTypeValue)) {
            return false;
        }
        MediaTypeValue other = (MediaTypeValue) obj;
        return Objects.equals(value, other.value);
    }
}
