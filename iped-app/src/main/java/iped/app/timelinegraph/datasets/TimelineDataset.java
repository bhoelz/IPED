package iped.app.timelinegraph.datasets;

import iped.data.IItemId;

import java.util.List;

public interface TimelineDataset {
    public List<IItemId> getItems(int item, int seriesId);

}
