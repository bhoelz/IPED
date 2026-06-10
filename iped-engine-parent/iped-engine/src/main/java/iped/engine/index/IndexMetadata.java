package iped.engine.index;

import iped.engine.lucene.analysis.FastASCIIFoldingFilter;
import iped.parsers.util.MetadataUtil;
import iped.utils.DateUtil;
import iped.utils.TomlProperties;
import org.apache.lucene.index.IndexableField;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.text.ParseException;
import java.util.Collections;
import java.util.Date;
import java.util.Enumeration;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

public final class IndexMetadata {

    private IndexMetadata() {
    }

    public static final String ATTR_TYPES_FILENAME = "metadataTypes.toml";

    private static final String SEED_RESOURCE = "iped/config/defaults/conf/" + ATTR_TYPES_FILENAME;

    public static Map<String, Class<?>> getMetadataTypes() {
        return Collections.unmodifiableMap(MetadataUtil.getMetadataTypes());
    }

    @SuppressWarnings("unchecked")
    public static void saveMetadataTypes(File confDir) throws IOException {
        File metadataTypesFile = new File(confDir, ATTR_TYPES_FILENAME);
        Map<String, Object> types = new TreeMap<>();
        for (Entry<String, Class<?>> e : MetadataUtil.getMetadataTypes().entrySet().toArray(new Entry[0])) {
            types.put(e.getKey(), e.getValue().getName());
        }
        TomlProperties.store(types, metadataTypesFile.toPath());
    }

    public static void loadMetadataTypes(File confDir) throws IOException, ClassNotFoundException {
        TomlProperties props = new TomlProperties();
        // seed with the built-in known types, then let the case file override them
        Enumeration<URL> seeds = IndexMetadata.class.getClassLoader().getResources(SEED_RESOURCE);
        while (seeds.hasMoreElements()) {
            try (InputStream is = seeds.nextElement().openStream()) {
                props.load(is);
            }
        }
        File metadataTypesFile = new File(confDir, ATTR_TYPES_FILENAME);
        if (metadataTypesFile.exists()) {
            props.load(metadataTypesFile.toPath());
        }
        for (String key : props.stringPropertyNames()) {
            MetadataUtil.setMetadataType(key, Class.forName(props.getProperty(key)));
        }
    }

    public static String normalize(String value, boolean toLowerCase) {
        if (toLowerCase) {
            value = value.toLowerCase();
        }
        char[] input = value.toCharArray();
        char[] output = new char[input.length * 4];
        int len = FastASCIIFoldingFilter.foldToASCII(input, 0, output, 0, input.length);
        return new String(output, 0, len).trim();
    }

    public static Object getCastedValue(Class<?> c, IndexableField f) throws ParseException {
        if (Date.class.equals(c)) {
            String value = f.stringValue();
            try {
                return DateUtil.stringToDate(value);
            } catch (ParseException e) {
                return DateUtil.tryToParseDate(value);
            }
        } else if (f.numericValue() != null) {
            Number num = f.numericValue();
            if (Byte.class.equals(c)) {
                return num.byteValue();
            } else if (Short.class.equals(c)) {
                return num.shortValue();
            } else if (Integer.class.equals(c)) {
                return num.intValue();
            } else if (Long.class.equals(c)) {
                return num.longValue();
            } else if (Float.class.equals(c)) {
                return num.floatValue();
            } else if (Double.class.equals(c)) {
                return num.doubleValue();
            } else {
                return num;
            }
        } else if (f.binaryValue() != null) {
            return f.binaryValue().bytes;
        } else {
            return f.stringValue();
        }
    }
}
