/*
 * Copyright 2020-2020, João Vitor de Sá Hauck
 *
 * This file is part of Indexador e Processador de Evidencias Digitais (IPED).
 *
 * IPED is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * IPED is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with IPED.  If not, see <http://www.gnu.org/licenses/>.
 */
package iped.parsers.telegram;

import iped.data.IItemReader;
import iped.search.IItemSearcher;
import iped.utils.IOUtil;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public class Util {

    private static final Set<String> zeroLengthHashes = new HashSet<String>();
    static {
        // Hashes of empty input (byte[0]), see issue #2157.
        zeroLengthHashes.add("d41d8cd98f00b204e9800998ecf8427e"); // MD5
        zeroLengthHashes.add("da39a3ee5e6b4b0d3255bfef95601890afd80709"); // SHA-1
        zeroLengthHashes.add("e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855"); // SHA-256
    }

    public static boolean isValidHash(String hash) {
        return hash != null && !hash.isBlank() && !zeroLengthHashes.contains(hash.toLowerCase());
    }

    public static void invertByteArray(byte[] array, int start, int len) {
        for (int i = 0; i < len / 2; i++) {
            byte aux = array[start + i];
            array[start + i] = array[start + len - i - 1];
            array[start + len - i - 1] = aux;
        }
    }

    public static String encodeBase64(byte[] data) {
        return java.util.Base64.getEncoder().encodeToString(data);
    }

    public static String readResourceAsString(String resource) {
        try {
            byte[] bytes = Util.class.getResourceAsStream(resource).readAllBytes();
            return new String(bytes, StandardCharsets.UTF_8);

        } catch (IOException e) {
            e.printStackTrace();
        }
        return "";
    }

    public static String readResourceAsBytes64(String resource) {
        try {
            byte[] bytes = Util.class.getResourceAsStream(resource).readAllBytes();
            if (bytes != null) return encodeBase64(bytes);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return "";
    }

    public static String getImageResourceAsEmbedded(String resource) {
        String ext = resource.substring(resource.lastIndexOf('.') + 1);
        return "data:image/" + ext + ";base64," + readResourceAsBytes64(resource);
    }

    // Report-path helpers — replicate iped.parsers.util.Util to avoid circular dep

    public static List<IItemReader> getItems(String query, IItemSearcher searcher) {
        if (searcher == null) return Collections.emptyList();
        return searcher.search(query);
    }

    public static String getExportPath(IItemReader item) {
        return getExportPath(item.getHash(), item.getType());
    }

    public static String getExportPath(String hash, String ext) {
        if (hash == null || hash.length() < 2) return "";
        StringBuilder sb = new StringBuilder();
        sb.append("../../").append(hash.charAt(0)).append("/").append(hash.charAt(1)).append("/").append(hash);
        if (ext != null && !ext.isEmpty()) sb.append(".").append(ext);
        return sb.toString();
    }

    public static Optional<String> getSourceFileIfExists(IItemReader item) {
        if (IOUtil.hasFile(item)) {
            File f = IOUtil.getFile(item);
            if (f != null && f.exists()) {
                Path p = f.toPath().toAbsolutePath().normalize();
                return Optional.of(adjustPath(p.toString()));
            }
        }
        return Optional.empty();
    }

    public static String getReportHref(IItemReader item) {
        String exportPath = getExportPath(item);
        String originalPath = getSourceFileIfExists(item).orElse("");
        String type = item.getMediaType() != null
                ? ((org.apache.tika.mime.MediaType) item.getMediaType()).getType()
                : "";
        String openMethod;
        if ("image".equals(type)) openMethod = "Image";
        else if ("audio".equals(type)) openMethod = "Audio";
        else if ("video".equals(type)) openMethod = "Video";
        else openMethod = "Other";
        return "javascript:open" + openMethod + "('" + exportPath + "','" + originalPath + "')";
    }

    private static String adjustPath(String path) {
        path = path.replace('\\', '/');
        if (path.startsWith("/")) path = path.substring(1);
        if (path.length() > 2 && path.charAt(1) == ':') path = "file:///" + path;
        return path;
    }

}
