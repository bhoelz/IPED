package iped.distributed;

import iped.distributed.kafka.DlqEntry;
import iped.distributed.kafka.TopicProvisioner;
import org.junit.jupiter.api.Test;

import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for the topic-naming conventions used by the DLQ subsystem.
 *
 * <p>These tests do not require a running Kafka broker; they exercise only the
 * static naming logic shared between {@link TopicProvisioner} and the DLQ manager.
 */
class DlqManagerTopicNamingTest {

    private static final String SUFFIX = ".dlq";

    // -------------------------------------------------------------------------
    // Stage topic → DLQ topic naming
    // -------------------------------------------------------------------------

    @Test
    void dlqTopicIsStageTopicPlusSuffix() {
        String stage = TopicProvisioner.stageTopic("mycase", 3);
        assertEquals("iped.mycase.stage.3", stage);
        assertEquals("iped.mycase.stage.3.dlq", stage + SUFFIX);
    }

    @Test
    void dlqTopicRoundTripThroughOriginalTopic() {
        for (int stage = 0; stage <= 5; stage++) {
            String stageTopic = TopicProvisioner.stageTopic("case-x", stage);
            String dlqTopic   = stageTopic + SUFFIX;
            assertEquals(stageTopic, DlqEntry.originalTopic(dlqTopic, SUFFIX),
                    "round-trip failed at stage " + stage);
        }
    }

    // -------------------------------------------------------------------------
    // DLQ topic discovery filter (simulated)
    // -------------------------------------------------------------------------

    @Test
    void filterIdentifiesDlqTopicsForCorrectCase() {
        String caseId = "case-abc";
        String prefix = TopicProvisioner.TOPIC_PREFIX + caseId + TopicProvisioner.STAGE_INFIX;

        // Simulate the broker returning a mix of topics from multiple cases
        Set<String> allTopics = Set.of(
                "iped.case-abc.stage.0",
                "iped.case-abc.stage.0.dlq",
                "iped.case-abc.stage.1.dlq",
                "iped.case-abc.stage.2.dlq",
                "iped.other-case.stage.0.dlq",   // different case — must be excluded
                "iped.case-abcx.stage.0.dlq",    // prefix must not match partial case ID
                "iped.status"
        );

        Set<String> dlqForCase = allTopics.stream()
                .filter(n -> n.startsWith(prefix) && n.endsWith(SUFFIX))
                .collect(Collectors.toSet());

        assertEquals(3, dlqForCase.size());
        assertTrue(dlqForCase.contains("iped.case-abc.stage.0.dlq"));
        assertTrue(dlqForCase.contains("iped.case-abc.stage.1.dlq"));
        assertTrue(dlqForCase.contains("iped.case-abc.stage.2.dlq"));
        assertFalse(dlqForCase.contains("iped.other-case.stage.0.dlq"),
                "must not include topics from a different case");
        assertFalse(dlqForCase.contains("iped.case-abcx.stage.0.dlq"),
                "must not match a case ID that is only a prefix of another");
    }

    @Test
    void filterExcludesNonDlqTopics() {
        String caseId = "demo";
        String prefix = TopicProvisioner.TOPIC_PREFIX + caseId + TopicProvisioner.STAGE_INFIX;

        Set<String> allTopics = Set.of(
                "iped.demo.stage.0",      // pipeline topic — NOT a DLQ
                "iped.demo.stage.0.dlq",  // DLQ
                "iped.demo.stage.1"       // pipeline topic — NOT a DLQ
        );

        Set<String> dlqForCase = allTopics.stream()
                .filter(n -> n.startsWith(prefix) && n.endsWith(SUFFIX))
                .collect(Collectors.toSet());

        assertEquals(Set.of("iped.demo.stage.0.dlq"), dlqForCase);
    }

    // -------------------------------------------------------------------------
    // OPS consumer group naming
    // -------------------------------------------------------------------------

    @Test
    void opsGroupNameIncludesCaseId() {
        String caseId = "case-99";
        String group  = iped.distributed.kafka.DlqManager.OPS_GROUP_PREFIX + caseId;
        assertTrue(group.startsWith("iped.dlq.ops."));
        assertTrue(group.endsWith(caseId));
    }

    @Test
    void opsGroupNamesAreUniquePerCase() {
        Set<String> groups = IntStream.range(0, 5)
                .mapToObj(i -> iped.distributed.kafka.DlqManager.OPS_GROUP_PREFIX + "case-" + i)
                .collect(Collectors.toSet());
        assertEquals(5, groups.size(), "each case must produce a distinct ops group name");
    }
}
