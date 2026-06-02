package iped.engine.search;

import iped.data.IItemId;
import iped.search.IMultiSearchResult;

import java.util.ArrayList;
import java.util.List;

public class ImageSimilarityLowScoreFilter {

    public static IMultiSearchResult filter(IMultiSearchResult result) {
        return filter(result, 1);
    }

    public static IMultiSearchResult filter(IMultiSearchResult result, float minScore) {
        ArrayList<IItemId> filteredItems = new ArrayList<IItemId>();
        ArrayList<Float> scores = new ArrayList<Float>();
        int len = result.getLength();
        for (int i = 0; i < len; i++) {
            float score = result.getScore(i);
            if (score > minScore) {
                filteredItems.add(result.getItem(i));
                scores.add(score);
            }
        }
        return new FilteredResult(filteredItems.toArray(new IItemId[0]), toPrimitive(scores));
    }

    private static float[] toPrimitive(List<Float> list) {
        if (list == null) {
            return null;
        }
        final float[] result = new float[list.size()];
        for (int i = 0; i < result.length; i++) {
            result[i] = list.get(i).floatValue();
        }
        return result;
    }

    private static final class FilteredResult implements IMultiSearchResult {
        private final IItemId[] items;
        private final float[] scores;

        private FilteredResult(IItemId[] items, float[] scores) {
            this.items = items;
            this.scores = scores;
        }

        @Override
        public IItemId getItem(int i) {
            return items[i];
        }

        @Override
        public iped.data.IIPEDSource getIPEDSource() {
            return null;
        }

        @Override
        public Iterable<IItemId> getIterator() {
            ArrayList<IItemId> list = new ArrayList<>(items.length);
            for (IItemId item : items) {
                list.add(item);
            }
            return list;
        }

        @Override
        public int getLength() {
            return items.length;
        }

        @Override
        public float getScore(int i) {
            return scores[i];
        }
    }
}
