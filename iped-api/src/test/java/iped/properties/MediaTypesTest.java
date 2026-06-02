package iped.properties;

import org.apache.tika.mime.MediaType;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class MediaTypesTest {

    @Test
    void getMediaTypeRegistry_whenCalled_thenReturnsRegistry() {
        assertNotNull(MediaTypes.getMediaTypeRegistry());
    }

    @Test
    void getParentType_whenUfedType_thenReturnsMetadataEntry() {
        MediaType ufedType = MediaType.application("x-ufed-call");

        assertEquals(MediaTypes.METADATA_ENTRY, MediaTypes.getParentType(ufedType));
    }

    @Test
    void isInstanceOf_whenUsingStringInput_thenFollowsRegistrySpecializationRules() {
        boolean result = MediaTypes.isInstanceOf("text/plain", MediaType.parse("text/plain"));

        assertFalse(result);
    }

    @Test
    void isInstanceOf_whenObjectNull_thenReturnsFalse() {
        assertFalse(MediaTypes.isInstanceOf((Object) null, MediaTypes.METADATA_ENTRY));
    }

    @Test
    void isMetadataEntryType_whenTypeProvided_thenFollowsSpecializationEvaluation() {
        assertFalse(MediaTypes.isMetadataEntryType(MediaTypes.METADATA_ENTRY));
        assertFalse(MediaTypes.isMetadataEntryType(MediaType.parse("text/plain")));
    }

}
