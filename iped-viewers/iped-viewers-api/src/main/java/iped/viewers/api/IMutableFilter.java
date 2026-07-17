package iped.viewers.api;

import java.util.ArrayList;
import java.util.Iterator;

/**
 * Interface that a filter must implements if it alters its internal state after added to
 * CombinedFilterer, so it won't cache its result.
 *
 * @author patrick.pdb
 */
public interface IMutableFilter {
  ArrayList<IFilterChangeListener> filterChangeListeners = new ArrayList<>();

  public default void addFilterChangeListener(IFilterChangeListener filterChangeListener) {
    filterChangeListeners.add(filterChangeListener);
  }

  public default void removeFilterChangeListener(IFilterChangeListener filterChangeListener) {
    filterChangeListeners.remove(filterChangeListener);
  }

  public default void fireFilterChangeListener() {
    for (Iterator iterator = filterChangeListeners.iterator(); iterator.hasNext(); ) {
      IFilterChangeListener filterChangeListener = (IFilterChangeListener) iterator.next();
      filterChangeListener.onFilterChange(this);
    }
  }
}
