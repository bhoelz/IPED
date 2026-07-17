package iped.scripting;

import iped.data.IItem;
import iped.io.SeekableInputStream;
import java.io.IOException;
import java.util.Collection;
import java.util.Date;

/**
 * Scripting-SDK accessor for reading item content and metadata without coupling scripts to
 * engine-internal item implementations.
 *
 * <p>Instances are obtained via {@link ICaseNavigator#accessor(IItem)}.
 *
 * @since 4.2
 */
public interface IItemAccessor {

  /**
   * Returns the stable UUID string for this item.
   *
   * @return UUID; never {@code null}
   */
  String getUUID();

  /**
   * Returns the item name (filename or derived label).
   *
   * @return item name; never {@code null}
   */
  String getName();

  /**
   * Returns the path of the item within the case (e.g. {@code /disk.E01/unallocated/file.doc}).
   *
   * @return path string; never {@code null}
   */
  String getPath();

  /**
   * Returns the MIME type string of this item as detected during processing.
   *
   * @return MIME type string such as {@code "application/pdf"}; may be {@code null} if type
   *     detection was not performed
   */
  String getMediaType();

  /**
   * Returns the file length in bytes, or {@code -1} if unknown.
   *
   * @return length ≥ 0, or -1
   */
  long getLength();

  /**
   * Returns the last-modified timestamp, or {@code null} if unavailable.
   *
   * @return modification date or {@code null}
   */
  Date getModificationDate();

  /**
   * Returns the creation timestamp, or {@code null} if unavailable.
   *
   * @return creation date or {@code null}
   */
  Date getCreationDate();

  /**
   * Returns the SHA-256 hash of the item content in hex, or {@code null} if not computed.
   *
   * @return lowercase hex SHA-256, or {@code null}
   */
  String getSha256();

  /**
   * Returns the value of an extra metadata attribute by its property name.
   *
   * @param propertyName attribute key; must not be {@code null}
   * @return value object, or {@code null} if not set
   */
  Object getExtraAttribute(String propertyName);

  /**
   * Returns all extra attribute keys set on this item.
   *
   * @return unmodifiable collection of key names; never {@code null}
   */
  Collection<String> getExtraAttributeNames();

  /**
   * Opens a seekable stream for reading the raw item content.
   *
   * <p>Callers are responsible for closing the stream.
   *
   * @return seekable input stream; never {@code null}
   * @throws IOException if the content cannot be opened
   */
  SeekableInputStream openStream() throws IOException;

  /**
   * Returns {@code true} if this item has been marked as deleted in the source (e.g. a deleted file
   * in a disk image).
   *
   * @return deletion flag
   */
  boolean isDeleted();

  /**
   * Returns {@code true} if this item is a directory/container (e.g. a folder or a compound file
   * entry that has children).
   *
   * @return directory flag
   */
  boolean isDir();
}
