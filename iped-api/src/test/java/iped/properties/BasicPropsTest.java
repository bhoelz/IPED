package iped.properties;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BasicPropsTest {

    @Test
    void set_whenInitialized_thenContainsCoreProperties() {
        assertTrue(BasicProps.SET.contains(BasicProps.ID));
        assertTrue(BasicProps.SET.contains(BasicProps.PATH));
        assertTrue(BasicProps.SET.contains(BasicProps.CONTENTTYPE));
    }

    @Test
    void set_whenInitialized_thenDoesNotContainTrackSpecificFields() {
        assertFalse(BasicProps.SET.contains(BasicProps.TRACK_ID));
        assertFalse(BasicProps.SET.contains(BasicProps.PARENT_TRACK_ID));
        assertFalse(BasicProps.SET.contains(BasicProps.CONTAINER_TRACK_ID));
    }
}

