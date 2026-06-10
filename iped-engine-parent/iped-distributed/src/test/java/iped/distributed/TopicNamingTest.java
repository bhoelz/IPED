package iped.distributed;

import iped.distributed.kafka.TopicProvisioner;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TopicNamingTest {

    @Test
    void stageTopicNameIsCorrect() {
        assertEquals("iped.my-case.stage.0", TopicProvisioner.stageTopic("my-case", 0));
        assertEquals("iped.my-case.stage.3", TopicProvisioner.stageTopic("my-case", 3));
    }

    @Test
    void stage0IsRawTopic() {
        String raw = TopicProvisioner.stageTopic("abc", 0);
        assertTrue(raw.endsWith(".stage.0"));
    }
}
