package iped.engine.lucene;

import iped.engine.config.ConfigurationManager;
import iped.engine.config.IndexTaskConfig;
import lombok.extern.slf4j.Slf4j;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.store.NIOFSDirectory;

import java.io.File;
import java.io.IOException;

@Slf4j
public class ConfiguredFSDirectory {


    public static FSDirectory open(File indexDir) throws IOException {
        IndexTaskConfig config = ConfigurationManager.get().findObject(IndexTaskConfig.class);

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
