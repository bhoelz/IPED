package iped.scripting;

import iped.data.IItemId;
import java.util.Set;

/**
 * Scripting-SDK service for tagging and un-tagging items.
 *
 * <p>Tags are arbitrary string labels attached to items. Each item may hold zero or more tags
 * simultaneously. Tag names are case-sensitive and must be non-null, non-empty, and contain no
 * {@code |} characters.
 *
 * @since 4.2
 */
public interface ITaggingService {

  /**
   * Adds the given tag to the item identified by {@code itemId}.
   *
   * @param itemId the target item; must not be {@code null}
   * @param tagName tag to apply; must not be {@code null} or empty
   * @throws IllegalArgumentException if {@code tagName} contains invalid characters
   */
  void addTag(IItemId itemId, String tagName);

  /**
   * Removes the given tag from the item, if it was previously applied. A no-op if the tag was not
   * set.
   *
   * @param itemId the target item; must not be {@code null}
   * @param tagName tag to remove; must not be {@code null}
   */
  void removeTag(IItemId itemId, String tagName);

  /**
   * Returns all tags currently applied to the item, or an empty set if none.
   *
   * @param itemId the target item; must not be {@code null}
   * @return unmodifiable set of tag names; never {@code null}
   */
  Set<String> getTags(IItemId itemId);

  /**
   * Returns all distinct tag names used anywhere in the case.
   *
   * @return unmodifiable set of tag names; never {@code null}
   */
  Set<String> getAllTagNames();

  /**
   * Returns all item IDs that carry the specified tag.
   *
   * @param tagName tag name to query; must not be {@code null}
   * @return unmodifiable set of item IDs; never {@code null}
   */
  Set<IItemId> getItemsWithTag(String tagName);
}
