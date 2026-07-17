package iped.scripting;

import iped.data.IItem;
import iped.data.IItemId;
import iped.search.IMultiSearchResult;
import iped.search.SearchQueryDefinition;

/**
 * Stable scripting-SDK entry point for querying a case and navigating results.
 *
 * <p>Implementations are provided by the engine and injected into JS/Python script contexts at
 * runtime. Scripts should never attempt to instantiate this interface directly.
 *
 * @since 4.2
 */
public interface ICaseNavigator {

  /**
   * Returns the unique name of this case (typically the output directory name).
   *
   * @return non-null case name
   */
  String getCaseName();

  /**
   * Returns the total number of items indexed in the case.
   *
   * @return item count ≥ 0
   */
  int getTotalItems();

  /**
   * Executes a Lucene query against the case index and returns matching items.
   *
   * @param query Lucene query string; must not be {@code null}
   * @return result set, possibly empty; never {@code null}
   * @throws iped.exception.QueryNodeException if the query syntax is invalid
   */
  IMultiSearchResult search(String query);

  /**
   * Executes a structured query definition against the case index.
   *
   * @param query query definition; must not be {@code null}
   * @return result set, possibly empty; never {@code null}
   */
  IMultiSearchResult search(SearchQueryDefinition query);

  /**
   * Retrieves a single item by its stable global identifier.
   *
   * @param itemId the item identifier; must not be {@code null}
   * @return the item, or {@code null} if not found
   */
  IItem getItem(IItemId itemId);

  /**
   * Returns an {@link IItemAccessor} for reading content and metadata of the given item without
   * directly importing engine types.
   *
   * @param item the item to access; must not be {@code null}
   * @return accessor; never {@code null}
   */
  IItemAccessor accessor(IItem item);
}
