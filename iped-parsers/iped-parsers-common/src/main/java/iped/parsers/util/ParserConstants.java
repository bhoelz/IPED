package iped.parsers.util;

/**
 * String constants used across parser modules. Centralised here so individual parser JARs (e.g.
 * {@code iped-parser-whatsapp}) do not need to depend on {@code iped-parsers-impl} just for these
 * values.
 */
public final class ParserConstants {

  private ParserConstants() {}

  /**
   * Metadata key used to override the detected MIME type of an embedded item. Corresponds to {@code
   * StandardParser.INDEXER_CONTENT_TYPE}.
   */
  public static final String INDEXER_CONTENT_TYPE = "Indexer-Content-Type";
}
