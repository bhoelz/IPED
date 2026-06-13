package iped.data;

import java.io.Closeable;
import java.io.File;
import java.util.List;
import java.util.Set;

/**
 * Read-only access to a single indexed IPED case.
 *
 * <p>An {@code IIPEDSource} wraps a Lucene index and all case metadata for one
 * processing output. To search across multiple sources at once, see
 * {@link IMultiBookmarks} and the multi-source searchers.
 *
 * <p>Callers are responsible for closing the source when done.
 */
public interface IIPEDSource extends Closeable {

    /** Relative path of the Lucene index directory within the case module dir. */
    String INDEX_DIR = "index"; //$NON-NLS-1$

    /** Relative path of the IPED module directory within the case folder. */
    String MODULE_DIR = "iped"; //$NON-NLS-1$

    /** File name of the Sleuth Kit database file inside the case module dir. */
    String SLEUTH_DB = "sleuth.db"; //$NON-NLS-1$

    /** {@inheritDoc} */
    @Override
    void close();

    /**
     * @return the Lucene {@code Analyzer} used for query parsing and indexing;
     *         returned as {@code Object} to avoid a compile-time Lucene dependency
     */
    Object getSearchAnalyzer();

    /**
     * @return the leaf-level Lucene {@code IndexReader}; returned as
     *         {@code Object} to avoid a compile-time Lucene dependency
     */
    Object getLeafIndexReader();

    /**
     * @return the atomic-level Lucene {@code IndexReader}; returned as
     *         {@code Object} to avoid a compile-time Lucene dependency
     */
    Object getAtomicIndexReader();

    /**
     * @return the case root folder (the folder that contains the {@code iped/}
     *         module directory)
     */
    File getCaseDir();

    /**
     * @return all leaf (non-parent) category names present in the case index
     */
    List<String> getLeafCategories();

    /**
     * Returns all categories that are descendants of the given ancestral
     * category in the category hierarchy.
     *
     * @param ancestral name of the parent category
     * @return set of descendant category names (possibly empty)
     */
    Set<String> getDescendantsCategories(String ancestral);

    /**
     * @return the set of extra-attribute names stored by processing tasks
     *         in this case
     */
    Set<String> getExtraAttributes();

    /**
     * Converts a Lucene document id to the stable IPED item id.
     *
     * @param luceneId Lucene-internal document id (not stable across index merges)
     * @return stable IPED item id
     */
    int getId(int luceneId);

    /**
     * @return the Lucene index directory for this case
     */
    File getIndex();

    /**
     * Returns the item with the given stable IPED id.
     *
     * @param id stable IPED item id
     * @return the item, or {@code null} if not found
     */
    IItem getItemByID(int id);

    /**
     * Returns the item at the given Lucene document id.
     *
     * @param docID Lucene document id
     * @return the item, or {@code null} if not found
     */
    IItem getItemByLuceneID(int docID);

    /**
     * @return all keyword strings indexed in this case
     */
    Set<String> getKeywords();

    /**
     * @return the highest stable item id in this case
     */
    int getLastId();

    /**
     * Returns the parent item id for the given item id.
     *
     * @param id stable IPED item id
     * @return parent id, or {@code -1} if the item has no parent
     */
    int getParentId(int id);

    /**
     * Converts an {@link IItemId} (source + item id pair) to the Lucene
     * document id in this source.
     *
     * @param itemId the item identifier
     * @return Lucene document id
     */
    int getLuceneId(IItemId itemId);

    /**
     * Converts a stable IPED item id to the Lucene document id.
     *
     * @param id stable IPED item id
     * @return Lucene document id
     */
    int getLuceneId(int id);

    /**
     * @return the bookmark store for this case
     */
    IBookmarks getBookmarks();

    /**
     * @return the IPED module directory ({@code <caseDir>/iped/})
     */
    File getModuleDir();

    /**
     * @return the multi-case bookmark store (may span multiple sources)
     */
    IMultiBookmarks getMultiBookmarks();

    /**
     * @return a handle to the underlying Lucene {@code IndexReader}; type is
     *         opaque to avoid a compile-time Lucene dependency
     */
    Object getIndexReaderHandle();

    /**
     * @return a handle to the underlying Lucene {@code IndexSearcher}; type is
     *         opaque to avoid a compile-time Lucene dependency
     */
    Object getIndexSearcherHandle();

    /**
     * @return the numeric source id assigned to this case within a multi-source
     *         session
     */
    int getSourceId();

    /**
     * @return total number of items in the case index
     */
    int getTotalItems();

    /**
     * @return UUIDs of all evidence sources that contributed items to this case
     */
    Set<String> getEvidenceUUIDs();
}
