package iped.engine.index;

import iped.engine.data.Item;
import iped.engine.util.Util;

import java.io.File;
import java.io.IOException;
import java.util.Set;

public final class IndexExtraAttributes {

    private IndexExtraAttributes() {
    }

    public static final String EXTRA_ATTRIBUTES_FILENAME = "extraAttributes.dat";

    public static void save(File output) throws IOException {
        File extraAttributtesFile = new File(output, "data/" + EXTRA_ATTRIBUTES_FILENAME);
        Set<String> extraAttr = Item.getAllExtraAttributes();
        Util.writeObject(extraAttr, extraAttributtesFile.getAbsolutePath());
        Util.fsync(extraAttributtesFile.toPath());
    }
}
