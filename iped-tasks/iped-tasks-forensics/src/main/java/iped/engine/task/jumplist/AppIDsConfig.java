package iped.engine.task.jumplist;

import iped.engine.config.AbstractTaskConfig;
import iped.utils.TomlProperties;

import java.io.IOException;
import java.nio.file.Path;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class AppIDsConfig extends AbstractTaskConfig<ConcurrentMap<String, String>> {

    private static final long serialVersionUID = 8409433427758336695L;

    public static final String CONFIG_FILE = "AppIDs.toml";

    private ConcurrentMap<String, String> appIDsMap = new ConcurrentHashMap<>();

    @Override
    public ConcurrentMap<String, String> getConfiguration() {
        return appIDsMap;
    }

    @Override
    public void setConfiguration(ConcurrentMap<String, String> config) {
        appIDsMap = config;
    }

    @Override
    public String getTaskEnableProperty() {
        return null;
    }

    @Override
    public String getTaskConfigFileName() {
        return CONFIG_FILE;
    }

    @Override
    public void processTaskConfig(Path resource) throws IOException {
        TomlProperties properties = new TomlProperties();
        properties.load(resource);
        for (String appID : properties.stringPropertyNames()) {
            appIDsMap.put(appID.toLowerCase(), properties.getProperty(appID));
        }
    }

}
