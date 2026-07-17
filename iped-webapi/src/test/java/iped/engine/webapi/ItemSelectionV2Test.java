package iped.engine.webapi;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;

public class ItemSelectionV2Test {

  @Test
  void resourceClassInstantiates() {
    assertNotNull(new ItemSelectionV2(), "ItemSelectionV2 resource should be instantiable");
  }
}
