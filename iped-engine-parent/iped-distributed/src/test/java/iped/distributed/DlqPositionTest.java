package iped.distributed;

import iped.distributed.kafka.DlqPosition;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class DlqPositionTest {

    @Test
    void constructorSetsAllFields() {
        DlqPosition pos = new DlqPosition("iped.case1.stage.3.dlq", 2, 99L);
        assertEquals("iped.case1.stage.3.dlq", pos.getDlqTopic());
        assertEquals(2,   pos.getPartition());
        assertEquals(99L, pos.getOffset());
    }

    @Test
    void settersAndGettersRoundTrip() {
        DlqPosition pos = new DlqPosition();
        pos.setDlqTopic("t");
        pos.setPartition(5);
        pos.setOffset(1000L);
        assertEquals("t",     pos.getDlqTopic());
        assertEquals(5,       pos.getPartition());
        assertEquals(1000L,   pos.getOffset());
    }

    @Test
    void toStringContainsTopic() {
        DlqPosition pos = new DlqPosition("my-topic.dlq", 0, 3L);
        assertTrue(pos.toString().contains("my-topic.dlq"));
    }

    @Test
    void defaultConstructorLeavesTopicNull() {
        DlqPosition pos = new DlqPosition();
        assertNull(pos.getDlqTopic());
        assertEquals(0,  pos.getPartition());
        assertEquals(0L, pos.getOffset());
    }
}
