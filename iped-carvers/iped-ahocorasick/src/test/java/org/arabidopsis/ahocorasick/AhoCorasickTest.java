package org.arabidopsis.ahocorasick;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class AhoCorasickTest {

    private static byte[] bytes(String s) {
        return s.getBytes(StandardCharsets.ISO_8859_1);
    }

    private static AhoCorasick treeOf(String... keywords) {
        AhoCorasick tree = new AhoCorasick();
        for (String k : keywords) {
            tree.add(bytes(k), k);
        }
        tree.prepare();
        return tree;
    }

    private static List<SearchResult> allResults(AhoCorasick tree, String text) {
        List<SearchResult> results = new ArrayList<>();
        Iterator<SearchResult> it = tree.search(bytes(text));
        while (it.hasNext()) {
            results.add(it.next());
        }
        return results;
    }

    @Test
    public void findsSingleKeyword() {
        AhoCorasick tree = treeOf("world");
        List<SearchResult> results = allResults(tree, "hello world");

        assertEquals(1, results.size());
        assertEquals(List.of("world"), results.get(0).getOutputs());
        // lastIndex is one past the last matching byte
        assertEquals(11, results.get(0).getLastIndex());
    }

    @Test
    public void findsAllOverlappingKeywords() {
        // classic Aho-Corasick example
        AhoCorasick tree = treeOf("he", "she", "his", "hers");
        List<SearchResult> results = allResults(tree, "ushers");

        // "she" and "he" end at index 4, "hers" ends at index 6
        List<String> found = new ArrayList<>();
        for (SearchResult r : results) {
            for (Object o : r.getOutputs()) {
                found.add((String) o);
            }
        }
        assertTrue(found.contains("she"), "should find 'she' in " + found);
        assertTrue(found.contains("he"), "should find 'he' in " + found);
        assertTrue(found.contains("hers"), "should find 'hers' in " + found);
        assertFalse(found.contains("his"), "'his' is not in 'ushers'");
    }

    @Test
    public void suffixMatchSharesResultWithLongerMatch() {
        AhoCorasick tree = treeOf("he", "she");
        List<SearchResult> results = allResults(tree, "she");

        // both keywords end at the same position and are reported together
        assertEquals(1, results.size());
        SearchResult r = results.get(0);
        assertEquals(3, r.getLastIndex());
        assertTrue(r.getOutputs().contains("she"));
        assertTrue(r.getOutputs().contains("he"));
    }

    @Test
    public void findsRepeatedMatches() {
        AhoCorasick tree = treeOf("ab");
        List<SearchResult> results = allResults(tree, "abab");

        assertEquals(2, results.size());
        assertEquals(2, results.get(0).getLastIndex());
        assertEquals(4, results.get(1).getLastIndex());
    }

    @Test
    public void noMatchYieldsNoResults() {
        AhoCorasick tree = treeOf("xyz");
        assertTrue(allResults(tree, "hello world").isEmpty());
        assertTrue(allResults(tree, "").isEmpty());
    }

    @Test
    public void handlesHighBytes() {
        // bytes >= 0x80 are negative in Java; the automaton must still match them
        AhoCorasick tree = new AhoCorasick();
        byte[] keyword = new byte[] { (byte) 0xFF, (byte) 0xD8, (byte) 0xFF };
        tree.add(keyword, "jpeg-soi");
        tree.prepare();

        byte[] text = new byte[] { 0x00, (byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0, 0x10 };
        Iterator<SearchResult> it = tree.search(text);

        assertTrue(it.hasNext());
        SearchResult r = it.next();
        assertEquals(List.of("jpeg-soi"), r.getOutputs());
        assertEquals(4, r.getLastIndex());
        assertFalse(it.hasNext());
    }

    @Test
    public void keywordAtStartAndEnd() {
        AhoCorasick tree = treeOf("aa");
        List<SearchResult> results = allResults(tree, "aabaa");

        assertEquals(2, results.size());
        assertEquals(2, results.get(0).getLastIndex());
        assertEquals(5, results.get(1).getLastIndex());
    }

    @Test
    public void addAfterPrepareThrows() {
        AhoCorasick tree = treeOf("a");
        assertThrows(IllegalStateException.class, () -> tree.add(bytes("b"), "b"));
    }

    @Test
    public void searchBeforePrepareThrows() {
        AhoCorasick tree = new AhoCorasick();
        tree.add(bytes("a"), "a");
        assertThrows(IllegalStateException.class, () -> tree.startSearch(bytes("a")));
    }

    @Test
    public void continueSearchReturnsNullWhenExhausted() {
        AhoCorasick tree = treeOf("a");
        SearchResult first = tree.startSearch(bytes("xa"));
        assertEquals(2, first.getLastIndex());
        assertNull(tree.continueSearch(first));
    }

    @Test
    public void continueSearch1MatchesOptimizedVersion() {
        AhoCorasick tree = treeOf("ab", "bc");
        String text = "zababcz";

        SearchResult r1 = tree.startSearch(bytes(text));
        SearchResult r2 = new SearchResult(tree.getRoot(), bytes(text), 0);
        List<Integer> optimized = new ArrayList<>();
        List<Integer> reference = new ArrayList<>();
        while (r1 != null) {
            optimized.add(r1.getLastIndex());
            r1 = tree.continueSearch(r1);
        }
        while ((r2 = tree.continueSearch1(r2)) != null) {
            reference.add(r2.getLastIndex());
        }
        assertEquals(reference, optimized);
    }
}
