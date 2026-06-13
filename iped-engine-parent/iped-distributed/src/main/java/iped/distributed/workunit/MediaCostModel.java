package iped.distributed.workunit;

import iped.distributed.kafka.KafkaItemMessage;

/**
 * Assigns a relative <b>processing-cost weight</b> to an item based on its evidence
 * type, used by {@link AdaptiveWorkUnitPlanner} to size work units.
 *
 * <p>The weight multiplies an item's byte size to produce its "weighted size": types
 * that are expensive to process <i>per byte</i> (archives that expand into thousands of
 * sub-items, videos that get transcoded/thumbnailed) fill a work unit faster and are
 * therefore batched into <i>smaller</i> groups, balancing per-agent load.  Cheap,
 * dense types (plain text, directories) batch into larger groups, amortising the
 * fixed per-message and per-poll overhead.
 *
 * <p>The model is deliberately a coarse heuristic — exact per-item cost is unknowable
 * before processing.  Weights are matched first on a normalised media type, then on the
 * file extension as a fallback for items whose type has not yet been detected (raw
 * stage-0 items often carry only an extension).
 */
public class MediaCostModel {

    /** Default weight for an unrecognised type. */
    public static final double DEFAULT_WEIGHT = 1.0;

    /** Directories and zero-length items: negligible processing cost. */
    public static final double CHEAP_WEIGHT = 0.1;

    private final double defaultWeight;

    public MediaCostModel() {
        this(DEFAULT_WEIGHT);
    }

    public MediaCostModel(double defaultWeight) {
        this.defaultWeight = defaultWeight;
    }

    /**
     * Returns the cost weight for an item. Never returns a value below {@link #CHEAP_WEIGHT}.
     */
    public double weight(KafkaItemMessage item) {
        if (item == null) return defaultWeight;
        if (item.isDir()) return CHEAP_WEIGHT;

        Long len = item.getLength();
        if (len == null || len == 0L) return CHEAP_WEIGHT;

        String type = normalise(item.getMediaType());
        if (type != null) {
            Double w = weightForMediaType(type);
            if (w != null) return w;
        }
        // Fall back to extension when the media type is absent (raw items).
        String ext = item.getExtension();
        if (ext != null) {
            Double w = weightForExtension(ext.toLowerCase());
            if (w != null) return w;
        }
        return defaultWeight;
    }

    // -----------------------------------------------------------------------

    private static Double weightForMediaType(String type) {
        if (type.startsWith("video/"))        return 4.0;   // transcode + thumbnails
        if (type.startsWith("image/"))        return 1.5;   // thumbnails, possible OCR
        if (type.startsWith("audio/"))        return 1.0;
        if (type.startsWith("text/"))         return 1.0;
        if (isArchive(type))                  return 3.0;   // expands into many sub-items
        if (isDiskImage(type))               return 3.5;   // volume/partition expansion
        if (type.equals("inode/directory"))   return CHEAP_WEIGHT;
        return null;
    }

    private static Double weightForExtension(String ext) {
        return switch (ext) {
            case "zip", "rar", "7z", "tar", "gz", "tgz", "bz2", "xz", "cab", "arj" -> 3.0;
            case "e01", "ex01", "dd", "raw", "img", "vmdk", "vhd", "vhdx"          -> 3.5;
            case "mp4", "avi", "mkv", "mov", "wmv", "flv", "webm", "mpg", "mpeg"    -> 4.0;
            case "jpg", "jpeg", "png", "gif", "bmp", "tiff", "heic"                 -> 1.5;
            case "txt", "log", "csv", "json", "xml", "html"                         -> 1.0;
            default -> null;
        };
    }

    private static boolean isArchive(String type) {
        return type.equals("application/zip")
                || type.equals("application/x-rar-compressed")
                || type.equals("application/x-7z-compressed")
                || type.equals("application/x-tar")
                || type.equals("application/gzip")
                || type.equals("application/x-bzip2")
                || type.equals("application/x-xz")
                || type.equals("application/java-archive");
    }

    private static boolean isDiskImage(String type) {
        return type.equals("application/x-ewf")
                || type.equals("application/x-raw-disk-image")
                || type.equals("application/x-vmdk")
                || type.startsWith("application/x-iped-disk");
    }

    /** Strips parameters (e.g. {@code "; charset=utf-8"}) and lowercases. */
    private static String normalise(String mediaType) {
        if (mediaType == null || mediaType.isBlank()) return null;
        String t = mediaType;
        int semi = t.indexOf(';');
        if (semi >= 0) t = t.substring(0, semi);
        return t.trim().toLowerCase();
    }
}
