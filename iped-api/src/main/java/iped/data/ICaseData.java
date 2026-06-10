package iped.data;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;

/**
 * (IPED INTERFACE) Interface that defines all case data.
 *
 * @author Wladimir Leite (GPINF/SP)
 * @author Nassif (GPINF/SP)
 */
public interface ICaseData extends Serializable {

    String TIMEZONE_INFO_KEY = "TimeZones";

    /**
     * Returns an object stored in the case.
     *
     * @param key object name
     * @return the object stored in the case
     */
    Object getCaseObject(String key);

    /**
     * Adds an object to the case.
     *
     * @param key object name
     * @param data object stored in the case
     */
    Object addCaseObject(String key, Object data);

    int getDiscoveredEvidences();

    /**
     * @return discovered data volume so far
     */
    long getDiscoveredVolume();

    void incDiscoveredEvidences(int inc);

    /**
     * @param volume size of the newly discovered item
     */
    void incDiscoveredVolume(Long volume);

    /**
     * Stores a generic object in the case.
     *
     * @param key object name to store
     * @param value object to store
     */
    void putCaseObject(String key, Object value);

    /**
     * Saves the current object to a file using direct serialization and GZIP compression.
     *
     * @param file file to save
     * @throws IOException file access error
     */
    void save(File file) throws IOException;

    /**
     * @param containsReport whether the case contains a report
     */
    void setContainsReport(boolean containsReport);

    /**
     * @return true if the case contains a report
     */
    boolean containsReport();

    boolean isIpedReport();

    void setIpedReport(boolean ipedReport);

}
