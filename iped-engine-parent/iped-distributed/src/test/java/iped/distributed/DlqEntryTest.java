package iped.distributed;

import static org.junit.jupiter.api.Assertions.*;

import iped.distributed.kafka.DlqEntry;
import iped.distributed.kafka.KafkaItemMessage;
import org.junit.jupiter.api.Test;

class DlqEntryTest {

  // -------------------------------------------------------------------------
  // DlqEntry.from() — factory
  // -------------------------------------------------------------------------

  @Test
  void fromPopulatesAllFields() {
    KafkaItemMessage msg = new KafkaItemMessage();
    msg.setItemUuid("uuid-1");
    msg.setPath("/evidence/file.txt");
    msg.setPipelineStage(2);
    msg.setAttempt(3);

    DlqEntry entry = DlqEntry.from("iped.case1.stage.2.dlq", 0, 42L, msg);

    assertEquals("iped.case1.stage.2.dlq", entry.getDlqTopic());
    assertEquals(0, entry.getPartition());
    assertEquals(42L, entry.getOffset());
    assertEquals("uuid-1", entry.getItemUuid());
    assertEquals("/evidence/file.txt", entry.getPath());
    assertEquals(2, entry.getPipelineStage());
    assertEquals(3, entry.getAttempt());
  }

  @Test
  void fromHandlesNullPathAndUuid() {
    KafkaItemMessage msg = new KafkaItemMessage(); // all fields default/null
    DlqEntry entry = DlqEntry.from("topic.dlq", 1, 0L, msg);
    assertNull(entry.getItemUuid());
    assertNull(entry.getPath());
  }

  // -------------------------------------------------------------------------
  // DlqEntry.originalTopic() — topic name derivation
  // -------------------------------------------------------------------------

  @Test
  void originalTopicStripsDefaultSuffix() {
    assertEquals("iped.case1.stage.2", DlqEntry.originalTopic("iped.case1.stage.2.dlq", ".dlq"));
  }

  @Test
  void originalTopicWorksWithCustomSuffix() {
    assertEquals("iped.case2.stage.0", DlqEntry.originalTopic("iped.case2.stage.0-dead", "-dead"));
  }

  @Test
  void originalTopicPreservesEntirePrefixWhenSuffixIsSubstring() {
    // The suffix ".dlq" appears only at the end — prefix must be intact
    String orig = DlqEntry.originalTopic("iped.dlq.stage.1.dlq", ".dlq");
    assertEquals("iped.dlq.stage.1", orig);
  }

  @Test
  void originalTopicThrowsWhenSuffixAbsent() {
    assertThrows(
        IllegalArgumentException.class, () -> DlqEntry.originalTopic("iped.case1.stage.2", ".dlq"));
  }

  @Test
  void originalTopicThrowsOnNullTopic() {
    assertThrows(IllegalArgumentException.class, () -> DlqEntry.originalTopic(null, ".dlq"));
  }

  @Test
  void originalTopicThrowsOnNullSuffix() {
    assertThrows(
        IllegalArgumentException.class,
        () -> DlqEntry.originalTopic("iped.case1.stage.2.dlq", null));
  }

  @Test
  void originalTopicThrowsOnEmptyTopicThatMatchesSuffixOnly() {
    // edge: topic IS the suffix — result would be empty string
    // The method permits this (empty string is valid as a "stripped" topic)
    // so we just verify no exception and the result is empty
    String orig = DlqEntry.originalTopic(".dlq", ".dlq");
    assertEquals("", orig);
  }

  // -------------------------------------------------------------------------
  // toString — smoke test
  // -------------------------------------------------------------------------

  @Test
  void toStringContainsKeyFields() {
    KafkaItemMessage msg = new KafkaItemMessage();
    msg.setItemUuid("abc");
    msg.setPath("/x");
    DlqEntry e = DlqEntry.from("t.dlq", 0, 7L, msg);
    String s = e.toString();
    assertTrue(s.contains("t.dlq"));
    assertTrue(s.contains("abc"));
    assertTrue(s.contains("7"));
  }
}
