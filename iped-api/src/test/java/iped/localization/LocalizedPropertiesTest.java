package iped.localization;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Tests for {@link LocalizedProperties} class. */
class LocalizedPropertiesTest {

  @Test
  @DisplayName("getLocalizedField with unknown field should return the field itself")
  void getLocalizedField_withUnknownField_shouldReturnFieldItself() {
    String result = LocalizedProperties.getLocalizedField("completely_unknown_field_xyz");
    assertEquals("completely_unknown_field_xyz", result);
  }

  @Test
  @DisplayName("getNonLocalizedField with unknown field should return the field itself")
  void getNonLocalizedField_withUnknownField_shouldReturnFieldItself() {
    String result = LocalizedProperties.getNonLocalizedField("completely_unknown_field_xyz");
    assertEquals("completely_unknown_field_xyz", result);
  }

  @Test
  @DisplayName("getLocalizedField should not return null")
  void getLocalizedField_shouldNeverReturnNull() {
    String result = LocalizedProperties.getLocalizedField("name");
    assertNotNull(result);
  }

  @Test
  @DisplayName("getNonLocalizedField should not return null")
  void getNonLocalizedField_shouldNeverReturnNull() {
    String result = LocalizedProperties.getNonLocalizedField("name");
    assertNotNull(result);
  }

  @Test
  @DisplayName("getLocalizedField and getNonLocalizedField should be inverse operations")
  void localizedAndNonLocalized_shouldBeInverse() {
    // Start with a known field name and verify the round-trip
    String original = "name";
    String localized = LocalizedProperties.getLocalizedField(original);
    assertNotNull(localized);
    String roundTrip = LocalizedProperties.getNonLocalizedField(localized);
    assertEquals(
        original,
        roundTrip,
        "Expected round-trip: getNonLocalizedField(getLocalizedField('name')) == 'name'");
  }
}
