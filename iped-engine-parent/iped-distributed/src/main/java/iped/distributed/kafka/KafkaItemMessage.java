package iped.distributed.kafka;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * Kafka message payload representing an IPED item in the distributed pipeline.
 *
 * <p>An item enters the pipeline at {@code stage = 0} (raw, from datasource readers) and advances
 * to {@code stage = N} after passing through each Task Agent. Between stages the full item state is
 * carried in this message so that every downstream agent can read the outputs of all previous
 * tasks.
 *
 * <p><b>Content access</b>: the actual binary content of the item is <em>not</em> embedded here.
 * Instead {@link #inputStreamFactoryClass} and {@link #inputStreamFactoryParams} carry enough
 * information for a {@code InputStreamFactoryRegistry} on any worker node to reconstruct the
 * appropriate {@code ISeekableInputStreamFactory}, provided that the datasource files are
 * accessible via the shared storage mount.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class KafkaItemMessage {

  // ---- Identity ----------------------------------------------------------

  /** IPED case identifier (UUID or slug). */
  private String caseId;

  /** Unique identifier assigned to this item upon entering the distributed pipeline. */
  private String itemUuid;

  /** Original integer ID assigned by the local engine. */
  private int localItemId;

  /** UUID of the datasource this item belongs to. */
  private String dataSourceUuid;

  /** Human-readable datasource name. */
  private String dataSourceName;

  // ---- Pipeline position -------------------------------------------------

  /**
   * Current pipeline stage. 0 = raw (just produced by a reader). Incremented by each Task Agent
   * after successful processing.
   */
  private int pipelineStage;

  /** Whether this item should be prioritised in agent internal queues. */
  private boolean priority;

  // ---- Work-unit grouping ------------------------------------------------

  /**
   * Opaque identifier of the {@code WorkUnit} batch this item belongs to, assigned by {@code
   * AdaptiveWorkUnitPlanner} at ingestion time. {@code null} when the item was produced outside a
   * planned batch (e.g. sub-items discovered mid-pipeline).
   */
  private String workUnitId;

  /**
   * Zero-based index of this item within its {@link #workUnitId} batch. Stable across retries (same
   * item always carries the same index for a given work unit).
   */
  private int workUnitIndex;

  // ---- Retry / DLQ ---------------------------------------------------------

  /** Number of failed processing attempts at the current stage (0 = first try). */
  private int attempt;

  /**
   * Epoch millis before which this message must not be processed. Set on retry re-publication to
   * implement exponential backoff; 0 = no delay.
   */
  private long notBeforeMs;

  // ---- Item properties ---------------------------------------------------

  private String path;
  private String name;
  private String extension;
  private Long length;
  private Long fileOffset;

  private boolean isDir;
  private boolean isDeleted;
  private boolean isCarved;
  private boolean isSubItem;
  private boolean isRoot;
  private boolean hasChildren;
  private boolean sumVolume;

  private Integer subitemId;
  private String parentItemUuid;
  private List<String> parentItemUuids;

  // ---- Dates -------------------------------------------------------------

  private Date accessDate;
  private Date creationDate;
  private Date modificationDate;
  private Date changeDate;

  // ---- Analysis state ----------------------------------------------------

  private String hash;
  private String mediaType;

  /** Tika Metadata: field name → list of values. */
  private Map<String, List<String>> metadata;

  /** IPED extra attributes (set by tasks). */
  private Map<String, Object> extraAttributes;

  // ---- Content reference -------------------------------------------------

  /**
   * Fully-qualified class name of the {@code ISeekableInputStreamFactory} that can provide the
   * item's binary content. Example: {@code "iped.engine.sleuthkit.SleuthkitInputStreamFactory"}
   */
  private String inputStreamFactoryClass;

  /**
   * Parameters needed to reconstruct the factory on any node. Example: {@code {dbPath:
   * "/mnt/nas/case/sleuth.db", objectId: "1234"}}
   */
  private Map<String, String> inputStreamFactoryParams;

  /** The item's ID as stored inside the datasource (e.g. Sleuthkit object ID). */
  private String idInDataSource;

  // ---- Case paths --------------------------------------------------------

  /** Shared output directory for the case, accessible from all nodes. */
  private String caseOutputPath;

  /** Root of the shared storage mount (NFS / S3) on this node. */
  private String sharedStorageRoot;

  /**
   * HMAC-SHA256 payload signature (hex), computed by the producing agent over the immutable
   * structural fields of this message via {@link iped.distributed.security.PayloadSigner}. {@code
   * null} when payload signing is disabled.
   */
  private String signature;

  // ---- Constructors / factory --------------------------------------------

  public KafkaItemMessage() {}

  // ---- Getters & Setters -------------------------------------------------

  public String getCaseId() {
    return caseId;
  }

  public void setCaseId(String v) {
    caseId = v;
  }

  public String getItemUuid() {
    return itemUuid;
  }

  public void setItemUuid(String v) {
    itemUuid = v;
  }

  public int getLocalItemId() {
    return localItemId;
  }

  public void setLocalItemId(int v) {
    localItemId = v;
  }

  public String getDataSourceUuid() {
    return dataSourceUuid;
  }

  public void setDataSourceUuid(String v) {
    dataSourceUuid = v;
  }

  public String getDataSourceName() {
    return dataSourceName;
  }

  public void setDataSourceName(String v) {
    dataSourceName = v;
  }

  public int getPipelineStage() {
    return pipelineStage;
  }

  public void setPipelineStage(int v) {
    pipelineStage = v;
  }

  public boolean isPriority() {
    return priority;
  }

  public void setPriority(boolean v) {
    priority = v;
  }

  public String getWorkUnitId() {
    return workUnitId;
  }

  public void setWorkUnitId(String v) {
    workUnitId = v;
  }

  public int getWorkUnitIndex() {
    return workUnitIndex;
  }

  public void setWorkUnitIndex(int v) {
    workUnitIndex = v;
  }

  public int getAttempt() {
    return attempt;
  }

  public void setAttempt(int v) {
    attempt = v;
  }

  public long getNotBeforeMs() {
    return notBeforeMs;
  }

  public void setNotBeforeMs(long v) {
    notBeforeMs = v;
  }

  public String getPath() {
    return path;
  }

  public void setPath(String v) {
    path = v;
  }

  public String getName() {
    return name;
  }

  public void setName(String v) {
    name = v;
  }

  public String getExtension() {
    return extension;
  }

  public void setExtension(String v) {
    extension = v;
  }

  public Long getLength() {
    return length;
  }

  public void setLength(Long v) {
    length = v;
  }

  public Long getFileOffset() {
    return fileOffset;
  }

  public void setFileOffset(Long v) {
    fileOffset = v;
  }

  public boolean isDir() {
    return isDir;
  }

  public void setDir(boolean v) {
    isDir = v;
  }

  public boolean isDeleted() {
    return isDeleted;
  }

  public void setDeleted(boolean v) {
    isDeleted = v;
  }

  public boolean isCarved() {
    return isCarved;
  }

  public void setCarved(boolean v) {
    isCarved = v;
  }

  public boolean isSubItem() {
    return isSubItem;
  }

  public void setSubItem(boolean v) {
    isSubItem = v;
  }

  public boolean isRoot() {
    return isRoot;
  }

  public void setRoot(boolean v) {
    isRoot = v;
  }

  public boolean isHasChildren() {
    return hasChildren;
  }

  public void setHasChildren(boolean v) {
    hasChildren = v;
  }

  public boolean isSumVolume() {
    return sumVolume;
  }

  public void setSumVolume(boolean v) {
    sumVolume = v;
  }

  public Integer getSubitemId() {
    return subitemId;
  }

  public void setSubitemId(Integer v) {
    subitemId = v;
  }

  public String getParentItemUuid() {
    return parentItemUuid;
  }

  public void setParentItemUuid(String v) {
    parentItemUuid = v;
  }

  public List<String> getParentItemUuids() {
    return parentItemUuids;
  }

  public void setParentItemUuids(List<String> v) {
    parentItemUuids = v;
  }

  public Date getAccessDate() {
    return accessDate;
  }

  public void setAccessDate(Date v) {
    accessDate = v;
  }

  public Date getCreationDate() {
    return creationDate;
  }

  public void setCreationDate(Date v) {
    creationDate = v;
  }

  public Date getModificationDate() {
    return modificationDate;
  }

  public void setModificationDate(Date v) {
    modificationDate = v;
  }

  public Date getChangeDate() {
    return changeDate;
  }

  public void setChangeDate(Date v) {
    changeDate = v;
  }

  public String getHash() {
    return hash;
  }

  public void setHash(String v) {
    hash = v;
  }

  public String getMediaType() {
    return mediaType;
  }

  public void setMediaType(String v) {
    mediaType = v;
  }

  public Map<String, List<String>> getMetadata() {
    return metadata;
  }

  public void setMetadata(Map<String, List<String>> v) {
    metadata = v;
  }

  public Map<String, Object> getExtraAttributes() {
    return extraAttributes;
  }

  public void setExtraAttributes(Map<String, Object> v) {
    extraAttributes = v;
  }

  public String getInputStreamFactoryClass() {
    return inputStreamFactoryClass;
  }

  public void setInputStreamFactoryClass(String v) {
    inputStreamFactoryClass = v;
  }

  public Map<String, String> getInputStreamFactoryParams() {
    return inputStreamFactoryParams;
  }

  public void setInputStreamFactoryParams(Map<String, String> v) {
    inputStreamFactoryParams = v;
  }

  public String getIdInDataSource() {
    return idInDataSource;
  }

  public void setIdInDataSource(String v) {
    idInDataSource = v;
  }

  public String getCaseOutputPath() {
    return caseOutputPath;
  }

  public void setCaseOutputPath(String v) {
    caseOutputPath = v;
  }

  public String getSharedStorageRoot() {
    return sharedStorageRoot;
  }

  public void setSharedStorageRoot(String v) {
    sharedStorageRoot = v;
  }

  public String getSignature() {
    return signature;
  }

  public void setSignature(String v) {
    signature = v;
  }

  @Override
  public String toString() {
    return "KafkaItemMessage{caseId='"
        + caseId
        + "', itemUuid='"
        + itemUuid
        + "', stage="
        + pipelineStage
        + ", path='"
        + path
        + "'}";
  }
}
