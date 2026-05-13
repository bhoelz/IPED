/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package iped.data;

import java.io.Closeable;
import java.io.File;
import java.util.List;
import java.util.Set;

/**
 *
 * @author WERNECK
 */
public interface IIPEDSource extends Closeable {

    String INDEX_DIR = "index"; //$NON-NLS-1$
    String MODULE_DIR = "iped"; //$NON-NLS-1$
    String SLEUTH_DB = "sleuth.db"; //$NON-NLS-1$

    @Override
    void close();

    Object getSearchAnalyzer();

    Object getLeafIndexReader();

    Object getAtomicIndexReader();

    File getCaseDir();

    List<String> getLeafCategories();

    Set<String> getDescendantsCategories(String ancestral);

    Set<String> getExtraAttributes();

    int getId(int luceneId);

    File getIndex();

    IItem getItemByID(int id);

    IItem getItemByLuceneID(int docID);

    Set<String> getKeywords();

    int getLastId();

    int getParentId(int id);

    int getLuceneId(IItemId itemId);

    int getLuceneId(int id);

    IBookmarks getBookmarks();

    File getModuleDir();

    IMultiBookmarks getMultiBookmarks();

    Object getIndexReaderHandle();

    Object getIndexSearcherHandle();

    int getSourceId();

    int getTotalItems();

    Set<String> getEvidenceUUIDs();

}
