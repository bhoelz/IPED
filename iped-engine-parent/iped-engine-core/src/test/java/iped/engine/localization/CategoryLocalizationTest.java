package iped.engine.localization;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Tests for {@link CategoryLocalization}. */
class CategoryLocalizationTest {

  @Test
  @DisplayName("getInstance should return non-null singleton")
  void getInstance_shouldReturnSingleton() {
    CategoryLocalization instance = CategoryLocalization.getInstance();
    assertNotNull(instance);
  }

  @Test
  @DisplayName("getInstance should return same instance on multiple calls")
  void getInstance_shouldReturnSameInstance() {
    CategoryLocalization first = CategoryLocalization.getInstance();
    CategoryLocalization second = CategoryLocalization.getInstance();
    assertSame(first, second);
  }

  @Test
  @DisplayName("getLocalizedCategory with unknown category should return the category itself")
  void getLocalizedCategory_unknownCategory_shouldReturnItself() {
    CategoryLocalization loc = CategoryLocalization.getInstance();
    String unknown = "completely_unknown_category_xyz_123";
    assertEquals(unknown, loc.getLocalizedCategory(unknown));
  }

  @Test
  @DisplayName("getNonLocalizedCategory with unknown category should return the category itself")
  void getNonLocalizedCategory_unknownCategory_shouldReturnItself() {
    CategoryLocalization loc = CategoryLocalization.getInstance();
    String unknown = "completely_unknown_category_xyz_123";
    assertEquals(unknown, loc.getNonLocalizedCategory(unknown));
  }

  @Test
  @DisplayName("getLocalizedCategory should not return null for unknown input")
  void getLocalizedCategory_shouldNeverReturnNull() {
    CategoryLocalization loc = CategoryLocalization.getInstance();
    assertNotNull(loc.getLocalizedCategory("any_category"));
  }

  @Test
  @DisplayName("getNonLocalizedCategory should not return null for unknown input")
  void getNonLocalizedCategory_shouldNeverReturnNull() {
    CategoryLocalization loc = CategoryLocalization.getInstance();
    assertNotNull(loc.getNonLocalizedCategory("any_category"));
  }
}
