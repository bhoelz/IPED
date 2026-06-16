package iped.engine.webapi;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;

public class ItemSelectionV2Test {

    @Test
    void resourceClassInstantiates() {
        assertNotNull(new ItemSelectionV2(), "ItemSelectionV2 resource should be instantiable");
    }
}
