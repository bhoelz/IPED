package iped.scripting;

import iped.data.IItemId;

import java.util.Set;

/**
 * Scripting-SDK service for managing bookmarks.
 *
 * <p>Bookmarks are named, ordered collections of items. Unlike tags (which are
 * attached to individual items), bookmarks are managed as named sets that can
 * group items for reporting and export.
 *
 * @since 4.2
 */
public interface IBookmarkService {

    /**
     * Creates a new, empty bookmark with the given name.
     * If a bookmark with that name already exists this is a no-op.
     *
     * @param bookmarkName name to create; must not be {@code null} or empty
     */
    void createBookmark(String bookmarkName);

    /**
     * Deletes the bookmark with the given name and removes all item associations.
     * A no-op if the bookmark does not exist.
     *
     * @param bookmarkName bookmark to delete; must not be {@code null}
     */
    void deleteBookmark(String bookmarkName);

    /**
     * Adds the item to the named bookmark. Creates the bookmark if it does not
     * already exist.
     *
     * @param bookmarkName bookmark name; must not be {@code null}
     * @param itemId       item to add; must not be {@code null}
     */
    void addToBookmark(String bookmarkName, IItemId itemId);

    /**
     * Removes the item from the named bookmark. A no-op if the item is not in
     * the bookmark or the bookmark does not exist.
     *
     * @param bookmarkName bookmark name; must not be {@code null}
     * @param itemId       item to remove; must not be {@code null}
     */
    void removeFromBookmark(String bookmarkName, IItemId itemId);

    /**
     * Returns the item IDs belonging to the named bookmark, or an empty set if
     * the bookmark does not exist.
     *
     * @param bookmarkName bookmark name; must not be {@code null}
     * @return unmodifiable set of item IDs; never {@code null}
     */
    Set<IItemId> getBookmarkItems(String bookmarkName);

    /**
     * Returns all existing bookmark names in this case.
     *
     * @return unmodifiable set of names; never {@code null}
     */
    Set<String> getBookmarkNames();

    /**
     * Returns the bookmarks that the given item belongs to.
     *
     * @param itemId item to query; must not be {@code null}
     * @return unmodifiable set of bookmark names; never {@code null}
     */
    Set<String> getBookmarksForItem(IItemId itemId);
}
