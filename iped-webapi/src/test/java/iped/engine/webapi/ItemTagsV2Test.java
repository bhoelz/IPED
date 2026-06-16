package iped.engine.webapi;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;

public class ItemTagsV2Test {

    @Test
    void resourceClassInstantiates() {
        assertNotNull(new ItemTagsV2(), "ItemTagsV2 resource should be instantiable");
    }
}
