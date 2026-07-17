package iped.engine.additionalindex;

import iped.datasource.IAdditionalDataSource;
import iped.datasource.IAdditionalDataSourceManager;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Default thread-safe implementation of {@link IAdditionalDataSourceManager}.
 *
 * <p>One instance is created per {@code IPEDSource} during case bootstrap. If a {@code
 * .additional-index/} directory exists inside the case's module directory, a {@link
 * LuceneAdditionalDataSource} is registered automatically.
 */
public class DefaultAdditionalDataSourceManager implements IAdditionalDataSourceManager {

  private final List<IAdditionalDataSource> sources = new CopyOnWriteArrayList<>();

  @Override
  public void register(IAdditionalDataSource source) {
    if (source == null)
      throw new IllegalArgumentException("source must not be null"); // $NON-NLS-1$
    sources.add(source);
  }

  @Override
  public List<IAdditionalDataSource> getSources() {
    return Collections.unmodifiableList(sources);
  }
}
