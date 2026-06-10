package org.arabidopsis.ahocorasick;

import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class InternalStructuresTest {

    @Test
    public void queueIsFifo() {
        Queue<String> q = new Queue<>();
        assertTrue(q.isEmpty());

        q.add("a");
        q.add("b");
        assertFalse(q.isEmpty());
        assertEquals("a", q.pop());

        q.add("c");
        assertEquals("b", q.pop());
        assertEquals("c", q.pop());
        assertTrue(q.isEmpty());
    }

    @Test
    public void poppingEmptyQueueThrows() {
        Queue<String> q = new Queue<>();
        assertThrows(IllegalStateException.class, q::pop);
    }

    @Test
    public void denseEdgeListStoresAndListsKeys() {
        DenseEdgeList list = new DenseEdgeList();
        State s1 = new State(1);
        State s2 = new State(1);

        assertNull(list.get((byte) 'a'));
        list.put((byte) 'a', s1);
        list.put((byte) 0xFF, s2);

        assertSame(s1, list.get((byte) 'a'));
        assertSame(s2, list.get((byte) 0xFF));

        byte[] keys = list.keys();
        assertEquals(2, keys.length);
        assertTrue(contains(keys, (byte) 'a'));
        assertTrue(contains(keys, (byte) 0xFF));
    }

    @Test
    public void sparseEdgeListStoresAndListsKeys() {
        SparseEdgeList list = new SparseEdgeList();
        State s1 = new State(1);
        State s2 = new State(1);

        assertNull(list.get((byte) 'x'));
        list.put((byte) 'x', s1);
        list.put((byte) 0x80, s2);

        assertSame(s1, list.get((byte) 'x'));
        assertSame(s2, list.get((byte) 0x80));

        byte[] keys = list.keys();
        assertEquals(2, keys.length);
        assertTrue(contains(keys, (byte) 'x'));
        assertTrue(contains(keys, (byte) 0x80));
    }

    @Test
    public void stateExtendBuildsTrie() {
        State root = new State(0);
        State leaf = root.extendAll("ab".getBytes());

        // root -> a -> b: three states in total
        assertEquals(3, root.size());
        // extending the same path reuses existing states
        assertSame(leaf, root.extendAll("ab".getBytes()));
        assertEquals(3, root.size());

        // extend() reuses an existing edge too
        State a = root.get((byte) 'a');
        assertSame(a, root.extend((byte) 'a'));
    }

    @Test
    public void stateOutputsAccumulate() {
        State s = new State(0);
        assertNull(s.getOutputs());
        s.addOutput("one");
        s.addOutput("two");
        assertEquals(Arrays.asList("one", "two"), s.getOutputs());
    }

    private static boolean contains(byte[] arr, byte b) {
        for (byte x : arr) {
            if (x == b) {
                return true;
            }
        }
        return false;
    }
}
