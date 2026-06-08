package iped.engine.config;

import iped.engine.util.Util;

import java.io.*;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class ExportByKeywordsConfig extends AbstractTaskConfig<List<String>> implements Externalizable {

    /**
     *
     */
    private static final long serialVersionUID = 2L;

    public static final String CONFIG_FILE = "KeywordsToExport.txt";

    /**
     * Enable property for the automatic file-export feature. Shared with
     * {@code ExportByCategoriesConfig}, which was moved to the
     * iped-tasks-forensics module and is therefore not visible from iped-engine.
     * This literal must stay in sync with that class's {@code ENABLE_PARAM}.
     */
    public static final String ENABLE_PARAM = "enableAutomaticExportFiles";

    private List<String> keywords = new ArrayList<>();

    @Override
    public boolean isEnabled() {
        return !this.keywords.isEmpty();
    }

    public List<String> getKeywords() {
        return this.keywords;
    }

    @Override
    public void processTaskConfig(Path resource) throws IOException {

        String content = Util.readUTF8Content(resource.toFile());
        for (String line : content.split("\n")) { //$NON-NLS-1$
            line = line.trim();
            if (line.startsWith("#") || line.isEmpty()) { //$NON-NLS-1$
                continue;
            }
            keywords.add(line);
        }

    }

    @Override
    public List<String> getConfiguration() {
        return keywords;
    }

    @Override
    public void setConfiguration(List<String> config) {
        this.keywords = config;
    }

    @Override
    public String getTaskEnableProperty() {
        return ENABLE_PARAM;
    }

    @Override
    public String getTaskConfigFileName() {
        return CONFIG_FILE;
    }

    @Override
    public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
        long l = in.readLong();
        if (l != serialVersionUID) {
            throw new InvalidClassException("SerialVersionUID not supported: " + l);
        }
        int size = in.readInt();
        keywords = new ArrayList<>();
        for (int i = 0; i < size; i++) {
            keywords.add(in.readUTF());
        }
    }

    @Override
    public void writeExternal(ObjectOutput out) throws IOException {
        out.writeLong(serialVersionUID);
        out.writeInt(keywords.size());
        for (String s : keywords) {
            out.writeUTF(s);
        }
    }

}
