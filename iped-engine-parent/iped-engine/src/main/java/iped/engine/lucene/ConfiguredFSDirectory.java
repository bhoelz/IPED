package iped.engine.lucene;

import iped.engine.config.ConfigurationManager;
import iped.engine.config.IndexSettings;
import java.io.File;
import java.io.IOException;
import lombok.extern.slf4j.Slf4j;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.store.NIOFSDirectory;

@Slf4j
public class ConfiguredFSDirectory {

  public static FSDirectory open(File indexDir) throws IOException {
    IndexSettings config = ConfigurationManager.get().findObjectInstanceOf(IndexSettings.class);

    FSDirectory result;
    if (config != null && config.isUseNIOFSDirectory()) {
      result = new NIOFSDirectory(indexDir.toPath());
    } else {
      result = FSDirectory.open(indexDir.toPath());
    }
    log.info("Using " + result.getClass().getSimpleName() + " to open index...");
    return result;
  }
}
