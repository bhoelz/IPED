package iped.properties;

import java.util.HashSet;
import java.util.Set;

/**
 * Names of the basic indexed properties every item has, used to build search queries and to read
 * item fields from the index.
 */
public class BasicProps {

  /** Item id within the case. */
  public static final String ID = "id"; // $NON-NLS-1$

  /** Id of the item's parent. */
  public static final String PARENTID = "parentId"; // $NON-NLS-1$

  /** Ids of all the item's ancestors. */
  public static final String PARENTIDs = "parentIds"; // $NON-NLS-1$

  /** UUID of the evidence (data source) the item belongs to. */
  public static final String EVIDENCE_UUID = "evidenceUUID"; // $NON-NLS-1$

  /** Item (file) name. */
  public static final String NAME = "name"; // $NON-NLS-1$

  /** Original file extension. */
  public static final String EXT = "ext"; // $NON-NLS-1$

  /** File type, derived from signature analysis. */
  public static final String TYPE = "type"; // $NON-NLS-1$

  /** File size in bytes. */
  public static final String LENGTH = "size"; // $NON-NLS-1$

  /** File creation date. */
  public static final String CREATED = "created"; // $NON-NLS-1$

  /** File last access date. */
  public static final String ACCESSED = "accessed"; // $NON-NLS-1$

  /** File last modification date. */
  public static final String MODIFIED = "modified"; // $NON-NLS-1$

  /** File metadata change date. */
  public static final String CHANGED = "changed"; // $NON-NLS-1$

  /** Full item path within the evidence. */
  public static final String PATH = "path"; // $NON-NLS-1$

  /** Categories assigned to the item. */
  public static final String CATEGORY = "category"; // $NON-NLS-1$

  /** Whether the item was deleted. */
  public static final String DELETED = "deleted"; // $NON-NLS-1$

  /** Extracted text content of the item. */
  public static final String CONTENT = "content"; // $NON-NLS-1$

  /** File hash (algorithm set by configuration). */
  public static final String HASH = "hash"; // $NON-NLS-1$

  /** Whether the item is a directory. */
  public static final String ISDIR = "isDir"; // $NON-NLS-1$

  /** Whether the item is an evidence root. */
  public static final String ISROOT = "isRoot"; // $NON-NLS-1$

  /** Whether the item has children, such as subitems or carved items. */
  public static final String HASCHILD = "hasChildren"; // $NON-NLS-1$

  /** Whether the item was recovered by carving. */
  public static final String CARVED = "carved"; // $NON-NLS-1$

  /** Whether the item is a subitem of a container. */
  public static final String SUBITEM = "subitem"; // $NON-NLS-1$

  /** Order of the subitem within its parent. */
  public static final String SUBITEMID = "subitemId"; // $NON-NLS-1$

  /** Offset of carved items within their parent. */
  public static final String OFFSET = "offset"; // $NON-NLS-1$

  /** Whether parsing the item timed out. */
  public static final String TIMEOUT = "timeout"; // $NON-NLS-1$

  /** Media type detected by signature analysis. */
  public static final String CONTENTTYPE = "contentType"; // $NON-NLS-1$

  /** Whether the item is a tree node (folder structure entry). */
  public static final String TREENODE = "treeNode"; // $NON-NLS-1$

  /** Whether the item has a thumbnail. */
  public static final String THUMB = "thumbnail"; // $NON-NLS-1$

  /** File system metadata address (e.g. MFT entry or inode number). */
  public static final String META_ADDRESS = "metaAddress";

  /** NTFS MFT entry sequence number. */
  public static final String MFT_SEQUENCE = "MFTSequence";

  /** Identifier of the file system containing the item. */
  public static final String FILESYSTEM_ID = "fileSystemId";

  /** Date of a timeline event related to the item. */
  public static final String TIMESTAMP = "timeStamp";

  /** Type of a timeline event related to the item. */
  public static final String TIME_EVENT = "timeEvent";

  /** Globally unique persistent id of the item. */
  public static final String TRACK_ID = "trackId";

  /** Persistent id ({@link #TRACK_ID}) of the item's parent. */
  public static final String PARENT_TRACK_ID = "parentTrackId";

  /** Persistent id ({@link #TRACK_ID}) of the container the item came from. */
  public static final String CONTAINER_TRACK_ID = "containerTrackId";

  /** The names of all basic properties indexed for every item. */
  public static final Set<String> SET = getBasicProps();

  private static Set<String> getBasicProps() {
    HashSet<String> basicProps = new HashSet<>();
    basicProps.add(ID);
    basicProps.add(PARENTID);
    basicProps.add(PARENTIDs);
    basicProps.add(EVIDENCE_UUID);
    basicProps.add(NAME);
    basicProps.add(EXT);
    basicProps.add(TYPE);
    basicProps.add(LENGTH);
    basicProps.add(CREATED);
    basicProps.add(ACCESSED);
    basicProps.add(MODIFIED);
    basicProps.add(CHANGED);
    basicProps.add(PATH);
    basicProps.add(CATEGORY);
    basicProps.add(DELETED);
    basicProps.add(CONTENT);
    basicProps.add(HASH);
    basicProps.add(ISDIR);
    basicProps.add(ISROOT);
    basicProps.add(HASCHILD);
    basicProps.add(CARVED);
    basicProps.add(SUBITEM);
    basicProps.add(SUBITEMID);
    basicProps.add(OFFSET);
    basicProps.add(TIMEOUT);
    basicProps.add(CONTENTTYPE);
    basicProps.add(TREENODE);
    basicProps.add(THUMB);
    basicProps.add(TIMESTAMP);
    basicProps.add(TIME_EVENT);
    return basicProps;
  }
}
