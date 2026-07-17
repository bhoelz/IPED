package iped.index.spi;

/**
 * Read-only, Lucene-free view of a single indexed document, handed to the caller during a {@link
 * IndexingPort#forEachDocument} scan. Field values are resolved lazily by name; the adapter hides
 * whether a field is backed by a stored field or by doc-values.
 *
 * <p>Valid only for the duration of the {@code Consumer} callback that receives it — do not retain
 * the instance past the callback (the underlying NRT reader is closed when the scan ends).
 */
public interface IndexedDocument {

  /**
   * String value of {@code field} for this document, resolved from a stored field or a sorted
   * doc-values field (adapter's choice), or {@code null} if the document has no value. Booleans are
   * returned as their string form (e.g. {@code "true"}), matching how they are indexed.
   */
  String getString(String field);

  /** Numeric doc-values value of {@code field} for this document, or {@code null} if absent. */
  Long getNumeric(String field);
}
