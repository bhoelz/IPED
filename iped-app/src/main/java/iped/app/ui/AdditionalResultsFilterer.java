package iped.app.ui;

import iped.data.IItemId;
import iped.engine.search.MultiSearchResult;
import iped.search.IMultiSearchResult;
import iped.viewers.api.IFilter;
import iped.viewers.api.IResultSetFilter;
import iped.viewers.api.IResultSetFilterer;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/** Result-set filter for items with at least one additional-task result. */
public final class AdditionalResultsFilterer implements IResultSetFilterer {

  private boolean enabled;
  private final IResultSetFilter filter =
      new IResultSetFilter() {
        @Override
        public IMultiSearchResult filterResult(IMultiSearchResult source) throws IOException {
          List<IItemId> ids = new ArrayList<>();
          List<Float> scores = new ArrayList<>();
          int index = 0;
          for (IItemId id : source.getIterator()) {
            if (App.get()
                    .appCase
                    .getAdditionalDataSourceManager()
                    .getAllExecutedTasks(id.getId())
                    .isEmpty()
                == false) {
              ids.add(id);
              scores.add(source.getScore(index));
            }
            index++;
          }
          float[] scoreArray = new float[scores.size()];
          for (int i = 0; i < scores.size(); i++) scoreArray[i] = scores.get(i);
          return new MultiSearchResult(ids.toArray(new IItemId[0]), scoreArray);
        }

        @Override
        public String toString() {
          return "Has additional task result";
        }
      };

  @Override
  public List<IFilter> getDefinedFilters() {
    return enabled ? List.of(filter) : List.of();
  }

  @Override
  public boolean hasFilters() {
    return true;
  }

  @Override
  public boolean hasFiltersApplied() {
    return enabled;
  }

  @Override
  public IFilter getFilter() {
    return enabled ? filter : null;
  }

  @Override
  public void clearFilter() {
    enabled = false;
  }

  public boolean isEnabled() {
    return enabled;
  }

  public void setEnabled(boolean enabled) {
    this.enabled = enabled;
  }

  @Override
  public String getFilterName() {
    return "Has additional task result";
  }
}
