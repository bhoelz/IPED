package iped.engine.search;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

import iped.data.IItemId;
import iped.engine.data.ItemId;
import iped.search.IMultiSearchResult;

class ImageSimilarityLowScoreFilterTest {

    @Test
    void shouldFilterByDefaultAndCustomThreshold() {
        IItemId[] ids = {new ItemId(1, 1), new ItemId(1, 2), new ItemId(1, 3)};
        float[] scores = {0.5f, 1.5f, 2.0f};
        IMultiSearchResult src = new SimpleResult(ids, scores);

        IMultiSearchResult defaultFiltered = ImageSimilarityLowScoreFilter.filter(src);
        assertEquals(2, defaultFiltered.getLength());
        assertEquals(2, defaultFiltered.getItem(0).getId());
        assertEquals(1.5f, defaultFiltered.getScore(0));

        IMultiSearchResult custom = ImageSimilarityLowScoreFilter.filter(src, 1.6f);
        assertEquals(1, custom.getLength());
        assertEquals(3, custom.getItem(0).getId());
        assertEquals(2.0f, custom.getScore(0));
        assertNull(custom.getIPEDSource());
    }

    private record SimpleResult(IItemId[] ids, float[] scores) implements IMultiSearchResult {
        @Override
        public IItemId getItem(int i) {
            return ids[i];
        }

        @Override
        public iped.data.IIPEDSource getIPEDSource() {
            return null;
        }

        @Override
        public Iterable<IItemId> getIterator() {
            return java.util.List.of(ids);
        }

        @Override
        public int getLength() {
            return ids.length;
        }

        @Override
        public float getScore(int i) {
            return scores[i];
        }
    }
}

