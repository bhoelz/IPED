package iped.engine.config;

import iped.configuration.Configurable;
import java.io.IOException;
import java.nio.file.DirectoryStream.Filter;
import java.nio.file.Path;

/**
 * Default {@link IndexSettings} for callers that only open an already-processed case to read
 * results (search, browse, view) and so never load the real {@code IndexTaskConfig} -- that class
 * lives in the {@code iped-tasks-storage-index} plugin module, which read-only callers must not
 * depend on.
 *
 * <p>Matches {@code IndexTaskConfig}'s own defaults. If a case was actually processed with
 * non-default IndexTaskConfig.toml settings (e.g. a custom maxTokenLength), query-time analysis
 * here won't match how that case's content was originally analyzed at index time.
 */
public class DefaultIndexSettings implements IndexSettings, Configurable<Void> {

  public static final DefaultIndexSettings INSTANCE = new DefaultIndexSettings();

  @Override
  public boolean isUseNIOFSDirectory() {
    return false;
  }

  @Override
  public boolean isForceMerge() {
    return false;
  }

  @Override
  public int getCommitIntervalSeconds() {
    return 1800;
  }

  @Override
  public int getMaxTokenLength() {
    return 255;
  }

  @Override
  public boolean isFilterNonLatinChars() {
    return false;
  }

  @Override
  public boolean isConvertCharsToAscii() {
    return false;
  }

  @Override
  public boolean isConvertCharsToLowerCase() {
    return false;
  }

  @Override
  public int[] getExtraCharsToIndex() {
    return null;
  }

  @Override
  public Filter<Path> getResourceLookupFilter() {
    return path -> false;
  }

  @Override
  public void processConfig(Path resource) throws IOException {
    // never called: getResourceLookupFilter() never matches anything.
  }

  @Override
  public Void getConfiguration() {
    return null;
  }

  @Override
  public void setConfiguration(Void config) {}
}
