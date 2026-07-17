package iped.engine.graph;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class DynLabelTest {

  @Test
  void constructor_storesName() {
    DynLabel label = new DynLabel("PERSON");
    assertEquals("PERSON", label.name());
  }

  @Test
  void name_returnsExactString() {
    DynLabel label = new DynLabel("MY_LABEL");
    assertEquals("MY_LABEL", label.name());
  }

  @Test
  void factory_label_createsInstance() {
    DynLabel label = DynLabel.label("PHONE");
    assertNotNull(label);
    assertEquals("PHONE", label.name());
  }

  @Test
  void factory_label_withEmptyString() {
    DynLabel label = DynLabel.label("");
    assertNotNull(label);
    assertEquals("", label.name());
  }

  @Test
  void toString_containsName() {
    DynLabel label = new DynLabel("EMAIL");
    String str = label.toString();
    assertNotNull(str);
    assertTrue(str.contains("EMAIL"), "toString should contain the label name");
  }

  @Test
  void twoLabels_withDifferentNames_areDistinct() {
    DynLabel l1 = DynLabel.label("PERSON");
    DynLabel l2 = DynLabel.label("ORGANIZATION");
    assertNotEquals(l1.name(), l2.name());
  }
}
