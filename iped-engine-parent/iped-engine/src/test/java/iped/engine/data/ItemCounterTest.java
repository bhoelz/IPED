package iped.engine.data;

import static org.junit.jupiter.api.Assertions.*;

import iped.engine.config.ConfigurationView;
import iped.engine.core.CaseContext;
import iped.engine.core.CaseContextThreadLocal;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class ItemCounterTest {

  @BeforeEach
  void setUp() {
    Item.setStartID(0);
  }

  @AfterEach
  void tearDown() {
    CaseContextThreadLocal.clear();
    Item.setStartID(0);
  }

  @Test
  void testGlobalCounterBackwardsCompatibility() {
    // Without case context, should use global counter
    int id1 = Item.getNextId();
    int id2 = Item.getNextId();
    int id3 = Item.getNextId();

    assertEquals(0, id1, "First ID should be 0");
    assertEquals(1, id2, "Second ID should be 1");
    assertEquals(2, id3, "Third ID should be 2");
  }

  @Test
  void testCaseSpecificCounters() {
    UUID case1Id = UUID.randomUUID();
    UUID case2Id = UUID.randomUUID();

    CaseContext case1Context = createTestCaseContext(case1Id);
    CaseContext case2Context = createTestCaseContext(case2Id);

    // Case 1 should have sequential IDs
    CaseContextThreadLocal.set(case1Context);
    int case1_id1 = Item.getNextId();
    int case1_id2 = Item.getNextId();
    int case1_id3 = Item.getNextId();

    assertEquals(0, case1_id1);
    assertEquals(1, case1_id2);
    assertEquals(2, case1_id3);

    // Switch to Case 2
    CaseContextThreadLocal.set(case2Context);
    int case2_id1 = Item.getNextId();
    int case2_id2 = Item.getNextId();

    assertEquals(0, case2_id1, "Case 2 should start at 0");
    assertEquals(1, case2_id2, "Case 2 should increment independently");

    // Switch back to Case 1
    CaseContextThreadLocal.set(case1Context);
    int case1_id4 = Item.getNextId();

    assertEquals(3, case1_id4, "Case 1 should continue from 3");
  }

  @Test
  void testSetStartIDPerCase() {
    UUID case1Id = UUID.randomUUID();
    UUID case2Id = UUID.randomUUID();

    CaseContext case1Context = createTestCaseContext(case1Id);
    CaseContext case2Context = createTestCaseContext(case2Id);

    // Set Case 1 to start at 100
    CaseContextThreadLocal.set(case1Context);
    Item.setStartID(100);
    int case1_id1 = Item.getNextId();
    int case1_id2 = Item.getNextId();

    assertEquals(100, case1_id1);
    assertEquals(101, case1_id2);

    // Set Case 2 to start at 200
    CaseContextThreadLocal.set(case2Context);
    Item.setStartID(200);
    int case2_id1 = Item.getNextId();

    assertEquals(200, case2_id1);

    // Case 1 should be unaffected
    CaseContextThreadLocal.set(case1Context);
    int case1_id3 = Item.getNextId();
    assertEquals(102, case1_id3);
  }

  @Test
  void testParallelCaseCounters() throws InterruptedException {
    UUID case1Id = UUID.randomUUID();
    UUID case2Id = UUID.randomUUID();

    int[] case1Results = new int[3];
    int[] case2Results = new int[3];

    Thread thread1 =
        new Thread(
            () -> {
              CaseContext context = createTestCaseContext(case1Id);
              CaseContextThreadLocal.set(context);

              case1Results[0] = Item.getNextId();
              case1Results[1] = Item.getNextId();
              case1Results[2] = Item.getNextId();
            });

    Thread thread2 =
        new Thread(
            () -> {
              CaseContext context = createTestCaseContext(case2Id);
              CaseContextThreadLocal.set(context);

              case2Results[0] = Item.getNextId();
              case2Results[1] = Item.getNextId();
              case2Results[2] = Item.getNextId();
            });

    thread1.start();
    thread2.start();
    thread1.join();
    thread2.join();

    // Each case should have its own sequence
    assertArrayEquals(new int[] {0, 1, 2}, case1Results);
    assertArrayEquals(new int[] {0, 1, 2}, case2Results);
  }

  @Test
  void testGetCounterForCase() {
    UUID caseId = UUID.randomUUID();
    CaseContext context = createTestCaseContext(caseId);

    // Counter shouldn't exist yet
    assertNull(Item.getCounterForCase(caseId));

    // Use the counter
    CaseContextThreadLocal.set(context);
    Item.getNextId();

    // Now it should exist
    assertNotNull(Item.getCounterForCase(caseId));
  }

  private CaseContext createTestCaseContext(UUID caseId) {
    // For Phase 1 testing, Manager and Statistics are null
    // They will be properly initialized in Phase 2/3
    return new CaseContext.Builder(caseId)
        .withCaseData(new CaseData())
        .withConfigurationView(new ConfigurationView())
        .build();
  }
}
