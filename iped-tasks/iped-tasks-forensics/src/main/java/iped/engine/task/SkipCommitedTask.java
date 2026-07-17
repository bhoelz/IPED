package iped.engine.task;

import iped.configuration.Configurable;
import iped.data.IItem;
import iped.engine.CmdLineArgs;
import iped.engine.config.ConfigurationManager;
import iped.engine.datasource.UfedXmlReader;
import iped.engine.task.carver.BaseCarveTask;
import iped.engine.task.index.IndexItem;
import iped.engine.util.Util;
import iped.exception.IPEDException;
import iped.index.spi.IndexingPort;
import iped.index.spi.IndexingSession;
import iped.properties.BasicProps;
import iped.utils.HashValue;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import lombok.extern.slf4j.Slf4j;

/**
 * Task to ignore already commited files into index. Commited containers without all their subitems
 * commited are not ignored to be processed again. Redefines ids and parentIds of incomming items to
 * be equal of commited items if they have same trackID.
 *
 * @author Luis Nassif
 */
@Slf4j
public class SkipCommitedTask extends AbstractTask {
  private static final String TEXT_SPLITTED = "textSplitted";

  public static final String PARENTS_WITH_LOST_SUBITEMS =
      SkipCommitDataKeys.PARENTS_WITH_LOST_SUBITEMS;

  public static final String DATASOURCE_NAMES = SkipCommitDataKeys.DATASOURCE_NAMES;

  public static final String trackID_ID_MAP = SkipCommitDataKeys.TRACK_ID_ID_MAP;

  public static final String IS_COMMITTED = SkipCommitDataKeys.IS_COMMITTED;

  private static HashValue[] commitedtrackIDs;

  private static Set<HashValue> parentsWithLostSubitems =
      Collections.synchronizedSet(new TreeSet<>());

  private static Set<HashValue> removedParents = Collections.synchronizedSet(new TreeSet<>());

  private static Map<HashValue, Integer> globalToIdMap = new HashMap<>();

  private static HashMap<String, String> prevRootNameToEvidenceUUID = new HashMap<>();

  private static CmdLineArgs args;

  private static AtomicBoolean inited = new AtomicBoolean();

  public static boolean isAlreadyCommited(IItem item) {
    if (commitedtrackIDs == null) {
      return false;
    }
    HashValue trackID = new HashValue(Util.getTrackID(item));
    boolean isCommitted = Arrays.binarySearch(commitedtrackIDs, trackID) >= 0;
    if (isCommitted) {
      item.setTempAttribute(IS_COMMITTED, Boolean.TRUE.toString());
    }
    return isCommitted;
  }

  @Override
  public List<Configurable<?>> getConfigurables() {
    return Collections.emptyList();
  }

  @Override
  public void init(ConfigurationManager configurationManager) throws Exception {

    if (inited.getAndSet(true)) {
      return;
    }

    IndexingPort indexingPort = worker.getIndexingPort();

    // All of init()'s reads are performed within a SINGLE session, i.e. against
    // ONE already-open NRT reader, instead of one reader open/close per
    // forEachDocument/distinctFieldValues call (each NRT open can trigger a
    // writer flush/segment merge -- see ADR-0002 addendum on IndexingSession).
    indexingPort.withSession(this::initFromSession);
  }

  private void initFromSession(IndexingSession session) throws IOException {

    session.forEachDocument(
        doc -> {
          String uuid = doc.getString(BasicProps.EVIDENCE_UUID);
          if (uuid != null && !prevRootNameToEvidenceUUID.containsValue(uuid)) {
            String path = doc.getString(BasicProps.PATH);
            prevRootNameToEvidenceUUID.put(Util.getRootName(path), uuid);
          }
        });

    args = (CmdLineArgs) caseData.getCaseObject(CmdLineArgs.class.getName());

    Set<String> evidenceNames =
        (Set<String>) caseData.getCaseObject(SkipCommitedTaskSupport.DATASOURCE_NAMES);
    for (String name : evidenceNames) {
      if (!args.isContinue() && prevRootNameToEvidenceUUID.containsKey(name))
        throw new IPEDException("Evidence name already exists in case: " + name);
    }

    if (!args.isContinue()) {
      return;
    }

    List<HashValue> trackIds = new ArrayList<>();
    session
        .distinctFieldValues(IndexItem.TRACK_ID)
        .forEach(trackID -> trackIds.add(new HashValue(trackID)));
    commitedtrackIDs = trackIds.toArray(new HashValue[0]);
    // Arrays.sort(commitedtrackIDs);

    session.forEachDocument(
        doc -> {
          String hashVal = doc.getString(IndexItem.PARENT_TRACK_ID);
          if (hashVal != null && !hashVal.isEmpty()) {
            HashValue persistParent = new HashValue(hashVal);
            Long prevParentId = doc.getNumeric(IndexItem.PARENTID);
            if (prevParentId != null && Arrays.binarySearch(commitedtrackIDs, persistParent) < 0) {
              globalToIdMap.put(persistParent, prevParentId.intValue());
            }
          }
          boolean hasChild = Boolean.valueOf(doc.getString(IndexItem.HASCHILD));
          boolean isDir = Boolean.valueOf(doc.getString(IndexItem.ISDIR));
          boolean isRoot = Boolean.valueOf(doc.getString(IndexItem.ISROOT));
          boolean isTexSplitted = Boolean.valueOf(doc.getString(TEXT_SPLITTED));
          Long prevId = doc.getNumeric(IndexItem.ID);
          String trackIdVal = doc.getString(IndexItem.TRACK_ID);
          if (prevId != null
              && trackIdVal != null
              && (hasChild || isDir || isRoot || isTexSplitted)) {
            HashValue trackID = new HashValue(trackIdVal);
            globalToIdMap.put(trackID, prevId.intValue());
          }
        });

    caseData.putCaseObject(trackID_ID_MAP, globalToIdMap);

    collectParentsWithoutAllSubitems(
        session, IndexItem.CONTAINER_TRACK_ID, ParsingTaskSupport.NUM_SUBITEMS);
    collectParentsWithoutAllSubitems(
        session, IndexItem.PARENT_TRACK_ID, BaseCarveTask.NUM_CARVED_AND_FRAGS);

    caseData.putCaseObject(PARENTS_WITH_LOST_SUBITEMS, parentsWithLostSubitems);

    log.info("Commited items: {}", commitedtrackIDs.length);
    log.info("Parents with lost subitems: {}", parentsWithLostSubitems.size());
  }

  /**
   * Counts, per parent-field string value, how many documents reference it (subject to the
   * subitem-count-inclusion rule below), then flags any potential parent (a document carrying
   * {@code subitemCountField}) whose expected subitem count does not match the number of references
   * found.
   *
   * <p>Relocated from a Lucene-ordinal-space implementation (referencingSubitems[ord] arrays keyed
   * by {@code parentContainers} term ordinal, {@code parentContainers.lookupTerm(...)}) to a
   * string-keyed map above the {@link IndexingPort}, per ADR-0002: the per-document scan is
   * unchanged, only the correlation is re-expressed without leaking Lucene ordinal/term APIs
   * through the port.
   *
   * <p>Restores the original pre-migration short-circuit: if {@code subitemCountField} (e.g. {@code
   * NUM_SUBITEMS}) is absent index-wide -- the common case early in processing, before any item has
   * produced it -- neither scan below can possibly flag a parent with lost subitems, so both
   * full-index passes are skipped entirely.
   */
  private void collectParentsWithoutAllSubitems(
      IndexingSession session, String parentIdField, String subitemCountField) throws IOException {
    if (!session.fieldExists(subitemCountField)) {
      return;
    }

    Map<String, Integer> referencingSubitems = new HashMap<>();
    Set<Integer> countedIds = new HashSet<>();

    session.forEachDocument(
        doc -> {
          Long longId = doc.getNumeric(IndexItem.ID);
          if (longId == null) {
            return;
          }
          int id = longId.intValue();
          String parentVal = doc.getString(parentIdField);
          if (parentVal != null && !countedIds.contains(id)) {
            boolean isSubitem = Boolean.valueOf(doc.getString(BasicProps.SUBITEM));
            if (parentIdField.equals(IndexItem.CONTAINER_TRACK_ID) || !isSubitem) {
              referencingSubitems.merge(parentVal, 1, Integer::sum);
            }
          }
          // splited items occur more than once, so we track seen ids
          countedIds.add(id);
        });

    session.forEachDocument(
        doc -> {
          Long subitemsCount = doc.getNumeric(subitemCountField);
          if (subitemsCount == null) {
            return;
          }
          String persistId = doc.getString(IndexItem.TRACK_ID);
          if (persistId == null) {
            return;
          }
          int carvedIgnored = 0;
          if (subitemCountField.equals(BaseCarveTask.NUM_CARVED_AND_FRAGS)) {
            carvedIgnored = stats.getCarvedIgnoredNum(new HashValue(persistId));
          }
          int references = referencingSubitems.getOrDefault(persistId, 0);
          if (subitemsCount != references + carvedIgnored) {
            parentsWithLostSubitems.add(new HashValue(persistId));
            // System.out.println("Parent with lost child " + persistId + " subitems " +
            // subitemsCount + " carvedIgnored " + carvedIgnored + " references " +
            // references);
          }
        });
  }

  @Override
  public void finish() throws Exception {
    commitedtrackIDs = null;
    parentsWithLostSubitems.clear();
    removedParents.clear();
    globalToIdMap.clear();
    prevRootNameToEvidenceUUID.clear();
  }

  // Check again parents that are going to be processed in later processing queues
  // to avoid ignoring them in a second pass in this task.
  public static void checkAgainLaterProcessedParents(IItem item) {
    HashValue trackID = new HashValue(Util.getTrackID(item));
    if (removedParents.remove(trackID)) {
      parentsWithLostSubitems.add(trackID);
    }
  }

  @Override
  protected void process(IItem item) throws Exception {

    // must be calculated first, in all cases, to allow recovering in the future
    HashValue trackID = new HashValue(Util.getTrackID(item));

    if (item.getExtraAttribute(IndexItem.PARENT_TRACK_ID) == null && !item.isRoot()) {
      // this property is needed when resuming processing to get a previous parent id
      // referenced by subitems which parents were not commited, then when
      // reprocessing parents, their id can be updated to the previous value, so
      // parent-child relationships will be preserved.
      throw new RuntimeException(IndexItem.PARENT_TRACK_ID + " must be stored for all items!");
    }

    if (!args.isContinue()) {
      return;
    }

    // ignore already committed items. If they are containers without all their
    // subitems committed, process again
    if (Arrays.binarySearch(commitedtrackIDs, trackID) >= 0) {
      // we must "remove" seen containers from set below. It is possible for the same
      // container to be enqueued twice: if it is a subItem/carved of some allocated
      // parent being processed again, coming from some datasource reader, AND if it
      // was already committed, coming from the index.
      if (!parentsWithLostSubitems.remove(trackID)) {
        item.setToIgnore(true);
        item.setTempAttribute(IS_COMMITTED, Boolean.TRUE.toString());
        return;
      } else {
        removedParents.add(trackID);
      }
    }

    // reset number of carved ignored subitems because parent will be processed
    // again
    stats.resetCarvedIgnored(item);

    // change evidenceUUID to previous processing evidenceUUID
    String rootPrefix = Util.getRootName(item.getPath());
    String oldUUID = prevRootNameToEvidenceUUID.get(rootPrefix);
    if (oldUUID != null) {
      if (caseData.getCaseObject(UfedXmlReader.MSISDN_PROP + oldUUID) == null) {
        synchronized (caseData) {
          Object msisdns =
              caseData.getCaseObject(UfedXmlReader.MSISDN_PROP + item.getDataSource().getUUID());
          caseData.putCaseObject(UfedXmlReader.MSISDN_PROP + oldUUID, msisdns);
        }
      }
      item.getDataSource().setUUID(oldUUID);
    }
  }
}
