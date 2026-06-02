package iped.engine.datasource;

import iped.data.ICaseData;
import iped.data.IItem;

/**
 * Engine-side support interface for datasource reader modules.
 *
 * Implementations live in iped-engine (Manager) and are registered via
 * DatasourceRegistry at startup. This breaks the compile-time dependency from
 * datasource modules onto iped-engine.
 */
public interface IDatasourceRegistry {

    IItem createItem();

    void addItem(IItem item) throws InterruptedException;

    void addItemFirst(IItem item) throws InterruptedException;

    /**
     * Computes the trackID for the item and, when resuming a previous processing,
     * reassigns the item ID to the one from the previous run. Must be called before
     * adding the item to the queue when --continue is active.
     */
    void prepareForQueue(IItem item, ICaseData caseData);

    String getTrackID(IItem item);

    void onProgress(String propertyName, Object oldValue, Object newValue);

    void setEnvVar(String key, String value);
}
