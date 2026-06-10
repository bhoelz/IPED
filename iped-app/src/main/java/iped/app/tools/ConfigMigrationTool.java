package iped.app.tools;

import iped.utils.TomlProperties;
import iped.utils.UTF8Properties;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * One-shot tool that converts a pre-4.4 configuration tree (Java properties
 * files with .txt extension) into the new deviation-only TOML format.
 *
 * For every legacy key=value file it loads the corresponding built-in default
 * from the classpath ({@code iped/config/defaults/...}) and writes a .toml
 * file containing ONLY the keys whose values differ from the defaults.
 * Line-list files (CategoriesToExpand.txt, ...) become TOML arrays. XML and
 * JSON files are copied unchanged. Comments are not carried over: the
 * documentation lives in the built-in defaults.
 *
 * Usage: java -classpath iped.jar iped.app.tools.ConfigMigrationTool
 * &lt;old-config-dir&gt; &lt;output-dir&gt;
 */
public class ConfigMigrationTool {

    private static final String DEFAULTS_PREFIX = "iped/config/defaults/";

    private static final Map<String, String> LINE_LIST_KEYS = Map.of(
            "CategoriesToExpand.txt", "categories",
            "CategoriesToExport.txt", "categories",
            "KeywordsToExport.txt", "keywords");

    private static final Pattern INT = Pattern.compile("-?(0|[1-9][0-9]*)");
    private static final Pattern FLOAT = Pattern.compile("-?(0|[1-9][0-9]*)\\.[0-9]+");

    private int converted = 0;
    private int copied = 0;
    private final List<String> unknownKeys = new ArrayList<>();

    public static void main(String[] args) throws IOException {
        if (args.length != 2) {
            System.err.println("Usage: ConfigMigrationTool <old-config-dir> <output-dir>");
            System.exit(1);
        }
        File oldDir = new File(args[0]);
        File outDir = new File(args[1]);
        if (!oldDir.isDirectory()) {
            System.err.println("Not a directory: " + oldDir);
            System.exit(1);
        }
        ConfigMigrationTool tool = new ConfigMigrationTool();
        tool.migrateTree(oldDir, outDir, "");
        System.out.println();
        System.out.println("Converted " + tool.converted + " file(s), copied " + tool.copied + " unchanged file(s).");
        if (!tool.unknownKeys.isEmpty()) {
            System.out.println("Keys not present in the built-in defaults (kept in output, please review):");
            tool.unknownKeys.forEach(k -> System.out.println("  " + k));
        }
    }

    private void migrateTree(File src, File dst, String relPath) throws IOException {
        File[] children = src.listFiles();
        if (children == null) {
            return;
        }
        for (File child : children) {
            String childRel = relPath.isEmpty() ? child.getName() : relPath + "/" + child.getName();
            if (child.isDirectory()) {
                migrateTree(child, new File(dst, child.getName()), childRel);
            } else if (child.getName().endsWith(".txt")) {
                migrateFile(child, dst, childRel);
            } else {
                // XML, JSON and other formats are kept as-is
                dst.mkdirs();
                java.nio.file.Files.copy(child.toPath(), new File(dst, child.getName()).toPath(),
                        java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                copied++;
            }
        }
    }

    private void migrateFile(File src, File dstDir, String relPath) throws IOException {
        String tomlName = src.getName().substring(0, src.getName().length() - 4) + ".toml";
        TomlProperties defaults = loadDefault(tomlName);

        Map<String, Object> out = new LinkedHashMap<>();
        String listKey = LINE_LIST_KEYS.get(src.getName());
        if (listKey != null) {
            List<String> items = new ArrayList<>();
            for (String line : java.nio.file.Files.readAllLines(src.toPath())) {
                line = line.trim();
                if (!line.isEmpty() && !line.startsWith("#")) {
                    items.add(line);
                }
            }
            List<String> defaultItems = defaults != null ? defaults.getListProperty(listKey) : List.of();
            items.removeAll(defaultItems); // layers are merged by union
            if (items.isEmpty()) {
                System.out.println("SKIP  " + relPath + " (no deviation from defaults)");
                return;
            }
            out.put(listKey, items);
        } else {
            Map<String, String> legacy = loadLegacy(src);
            for (String key : legacy.keySet()) {
                String value = legacy.get(key).trim();
                String defaultValue = defaults != null ? defaults.getProperty(key) : null;
                if (defaultValue == null && defaults != null) {
                    unknownKeys.add(relPath + ": " + key);
                }
                if (defaultValue == null || !sameValue(defaultValue, value)) {
                    out.put(key, typed(value));
                }
            }
            if (out.isEmpty()) {
                System.out.println("SKIP  " + relPath + " (no deviation from defaults)");
                return;
            }
        }
        dstDir.mkdirs();
        File dst = new File(dstDir, tomlName);
        TomlProperties.store(out, dst.toPath());
        System.out.println("OK    " + relPath + " -> " + tomlName + " (" + out.size() + " deviating entries)");
        converted++;
    }

    private static boolean sameValue(String defaultValue, String legacyValue) {
        if (defaultValue.equals(legacyValue)) {
            return true;
        }
        // semicolon separated lists: compare item by item, ignoring whitespace
        if (defaultValue.contains(";") && legacyValue.contains(";")) {
            return splitList(defaultValue).equals(splitList(legacyValue));
        }
        return false;
    }

    private static List<String> splitList(String value) {
        List<String> items = new ArrayList<>();
        for (String item : value.split(";")) {
            if (!item.trim().isEmpty()) {
                items.add(item.trim());
            }
        }
        return items;
    }

    /**
     * Loads a legacy key=value file. Unlike plain {@link UTF8Properties},
     * repeated keys (used by e.g. MakePreviewConfig.txt to span long lists over
     * several lines) are accumulated into a single semicolon separated value.
     */
    private static Map<String, String> loadLegacy(File src) throws IOException {
        Map<String, String> result = new LinkedHashMap<>();
        for (String line : java.nio.file.Files.readAllLines(src.toPath())) {
            String trimmed = line.trim();
            if (trimmed.isEmpty() || trimmed.startsWith("#")) {
                continue;
            }
            int pos = line.indexOf('=');
            while (pos > 0 && line.charAt(pos - 1) == '\\') {
                pos = line.indexOf('=', pos + 1);
            }
            if (pos <= 0) {
                continue;
            }
            String key = line.substring(0, pos).replace("\\=", "=").trim();
            String value = line.substring(pos + 1).replace("\\=", "=").trim();
            result.merge(key, value, (a, b) -> a + "; " + b);
        }
        return result;
    }

    private static Object typed(String value) {
        if (value.equalsIgnoreCase("true") || value.equalsIgnoreCase("false")) {
            return Boolean.parseBoolean(value);
        }
        if (INT.matcher(value).matches()) {
            try {
                return Long.parseLong(value);
            } catch (NumberFormatException ignore) {
            }
        }
        if (FLOAT.matcher(value).matches()) {
            return Double.parseDouble(value);
        }
        if (value.contains(";")) {
            List<String> items = new ArrayList<>();
            for (String item : value.split(";")) {
                if (!item.trim().isEmpty()) {
                    items.add(item.trim());
                }
            }
            if (items.size() > 1) {
                return items;
            }
        }
        return value;
    }

    private static TomlProperties loadDefault(String tomlName) throws IOException {
        ClassLoader cl = ConfigMigrationTool.class.getClassLoader();
        TomlProperties defaults = null;
        for (String candidate : new String[] { DEFAULTS_PREFIX + tomlName, DEFAULTS_PREFIX + "conf/" + tomlName }) {
            Enumeration<URL> urls = cl.getResources(candidate);
            while (urls.hasMoreElements()) {
                if (defaults == null) {
                    defaults = new TomlProperties();
                }
                try (InputStream is = urls.nextElement().openStream()) {
                    defaults.load(is);
                }
            }
        }
        return defaults;
    }
}
