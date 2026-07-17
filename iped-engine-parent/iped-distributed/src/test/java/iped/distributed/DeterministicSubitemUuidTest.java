package iped.distributed;

import static org.junit.jupiter.api.Assertions.*;

import iped.distributed.kafka.ItemConverter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

/**
 * Verifies that sub-item UUIDs are deterministic — same parent UUID + same ordinal always yields
 * the same sub-item UUID — which is the idempotency guarantee needed so that re-delivering a parent
 * item on agent restart does not create duplicate evidence items in the case index.
 */
class DeterministicSubitemUuidTest {

  private static final String PARENT_A = "3f6c1b2a-0000-0000-0000-aaaaaaaaaaaa";
  private static final String PARENT_B = "7e9d4c3b-0000-0000-0000-bbbbbbbbbbbb";

  // -------------------------------------------------------------------------
  // Stability: same inputs → same UUID
  // -------------------------------------------------------------------------

  @Test
  void sameParentAndOrdinalProduceSameUuid() {
    String first = ItemConverter.deterministicSubitemUuid(PARENT_A, 0);
    String second = ItemConverter.deterministicSubitemUuid(PARENT_A, 0);
    assertEquals(first, second, "same parent + ordinal must always yield the same UUID");
  }

  @Test
  void uuidIsStableAcrossMultipleCalls() {
    for (int i = 0; i < 5; i++) {
      String expected = ItemConverter.deterministicSubitemUuid(PARENT_A, i);
      assertEquals(
          expected,
          ItemConverter.deterministicSubitemUuid(PARENT_A, i),
          "UUID for ordinal " + i + " must be stable");
    }
  }

  // -------------------------------------------------------------------------
  // Uniqueness: different inputs → different UUIDs
  // -------------------------------------------------------------------------

  @Test
  void differentOrdinalsDifferentUuids() {
    String u0 = ItemConverter.deterministicSubitemUuid(PARENT_A, 0);
    String u1 = ItemConverter.deterministicSubitemUuid(PARENT_A, 1);
    assertNotEquals(u0, u1, "different ordinals must not collide");
  }

  @Test
  void differentParentsDifferentUuids() {
    String uA = ItemConverter.deterministicSubitemUuid(PARENT_A, 0);
    String uB = ItemConverter.deterministicSubitemUuid(PARENT_B, 0);
    assertNotEquals(uA, uB, "same ordinal under different parents must not collide");
  }

  @Test
  void noDuplicatesAcross1000SubitemsOfOneParent() {
    Set<String> uuids = new HashSet<>();
    for (int i = 0; i < 1000; i++) {
      String uuid = ItemConverter.deterministicSubitemUuid(PARENT_A, i);
      assertTrue(uuids.add(uuid), "collision at ordinal " + i);
    }
  }

  // -------------------------------------------------------------------------
  // Re-delivery simulation: two "runs" of the same parent produce identical sequences
  // -------------------------------------------------------------------------

  @Test
  void redeliveryProducesIdenticalSubitemUuidSequence() {
    int subitemCount = 10;

    // First run (original processing)
    List<String> firstRunUuids = collectSubitemUuids(PARENT_A, subitemCount);

    // Second run (re-delivery after agent restart — simulated by repeating the call)
    List<String> secondRunUuids = collectSubitemUuids(PARENT_A, subitemCount);

    assertEquals(
        firstRunUuids,
        secondRunUuids,
        "re-delivery of a parent must produce the same sub-item UUID sequence");
  }

  @Test
  void twoParentsWithSameSubitemCountProduceDifferentSequences() {
    List<String> uuidsA = collectSubitemUuids(PARENT_A, 5);
    List<String> uuidsB = collectSubitemUuids(PARENT_B, 5);
    assertNotEquals(uuidsA, uuidsB, "sub-item sequences from different parents must not be equal");
  }

  // -------------------------------------------------------------------------
  // Format: result must be a valid UUID string
  // -------------------------------------------------------------------------

  @Test
  void resultIsValidUuidString() {
    String uuid = ItemConverter.deterministicSubitemUuid(PARENT_A, 42);
    assertNotNull(uuid);
    // Validates UUID format: 8-4-4-4-12 hex groups
    assertTrue(
        uuid.matches("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}"),
        "result must be a lower-case UUID string, got: " + uuid);
  }

  // -------------------------------------------------------------------------
  // Helper
  // -------------------------------------------------------------------------

  private static List<String> collectSubitemUuids(String parentUuid, int count) {
    List<String> uuids = new ArrayList<>(count);
    for (int i = 0; i < count; i++) {
      uuids.add(ItemConverter.deterministicSubitemUuid(parentUuid, i));
    }
    return uuids;
  }
}
