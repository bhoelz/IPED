package iped.engine.config;

import iped.utils.TomlProperties;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class MakePreviewConfig extends AbstractTaskConfig<List<Set<String>>> {

    /**
     *
     */
    private static final long serialVersionUID = 1L;

    private static final String ENABLE_PROP = ParsingTaskConfig.ENABLE_PARAM;

    private static final String CONFIG_FILE = "MakePreviewConfig.toml";

    private static final String SUPPORTED_KEY = "supportedMimes";

    private static final String SUPPORTED_LINKS_KEY = "supportedMimesWithLinks";

    private Set<String> supportedMimes = new HashSet<>();

    private Set<String> supportedMimesWithLinks = new HashSet<>();

    public Set<String> getSupportedMimes() {
        return supportedMimes;
    }

    public Set<String> getSupportedMimesWithLinks() {
        return supportedMimesWithLinks;
    }

    @Override
    public List<Set<String>> getConfiguration() {
        return Arrays.asList(supportedMimes, supportedMimesWithLinks);
    }

    @Override
    public void setConfiguration(List<Set<String>> config) {
        supportedMimes = config.get(0);
        supportedMimesWithLinks = config.get(1);
    }

    @Override
    public String getTaskEnableProperty() {
        return ENABLE_PROP;
    }

    @Override
    public String getTaskConfigFileName() {
        return CONFIG_FILE;
    }

    @Override
    public void processTaskConfig(Path resource) throws IOException {
        TomlProperties properties = new TomlProperties();
        properties.load(resource);
        supportedMimes.addAll(properties.getListProperty(SUPPORTED_KEY));
        supportedMimesWithLinks.addAll(properties.getListProperty(SUPPORTED_LINKS_KEY));
    }

}
