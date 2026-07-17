package iped.tasks.cli;

import iped.configuration.Configurable;
import iped.data.IItem;
import iped.engine.config.ConfigurationManager;
import iped.engine.task.AbstractTask;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.apache.tika.metadata.Metadata;

/**
 * Terminal task in the standalone pipeline: {@code TaskExecutor} sets this as {@code nextTask} for
 * the task actually being run, so it receives the item right after that task finished with it
 * (mirroring how {@code iped.engine.task.additional.AdditionalIndexTask} terminates the
 * additional-processing pipeline). Captures metadata/extra attributes into plain maps instead of
 * persisting anywhere, since the item is disposed right after this task returns (see {@code
 * AbstractTask.sendToNextTask}).
 *
 * <p>Runs single-threaded, one item at a time (see {@code TaskExecutor}), so plain instance fields
 * -- no synchronization -- are enough to hand the captured result back after each {@code
 * processAndSendToNextTask} call.
 */
public class CollectingSinkTask extends AbstractTask {

  private Map<String, String[]> lastMetadata = Collections.emptyMap();
  private Map<String, Object> lastExtraAttributes = Collections.emptyMap();

  @Override
  public List<Configurable<?>> getConfigurables() {
    return List.of();
  }

  @Override
  public void init(ConfigurationManager configurationManager) {
    // nothing to initialize
  }

  @Override
  public void finish() {
    // nothing to release
  }

  @Override
  protected void process(IItem evidence) {
    Metadata metadata = (Metadata) evidence.getMetadata();
    Map<String, String[]> metadataMap = new LinkedHashMap<>();
    if (metadata != null) {
      for (String name : metadata.names()) {
        metadataMap.put(name, metadata.getValues(name));
      }
    }
    lastMetadata = metadataMap;
    lastExtraAttributes = new LinkedHashMap<>(evidence.getExtraAttributeMap());
  }

  Map<String, String[]> takeMetadata() {
    return lastMetadata;
  }

  Map<String, Object> takeExtraAttributes() {
    return lastExtraAttributes;
  }
}
