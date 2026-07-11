package iped.tasks.cli;

import iped.data.IItem;
import iped.engine.task.additional.AdditionalTaskWorker;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Redirects {@code worker.processNewItem()} -- normally used by carving tasks
 * (see {@code iped.engine.task.carver.BaseCarveTask#addOffsetFile}) to enqueue
 * a carved child for full-pipeline reprocessing -- to instead dump each
 * child's raw bytes plus minimal sidecar metadata into a flat output
 * directory. Deliberately does NOT change {@link AdditionalTaskWorker}'s
 * default behavior (still throws {@link UnsupportedOperationException} for
 * any task not explicitly run through this worker): this is a separate,
 * additive subclass that {@code TaskExecutor} only uses when the requested
 * task is one of the carving family AND the caller passed
 * {@code --extract-children-to}.
 *
 * <p><b>Limitation: one level of extraction only.</b> Each carved child is
 * written to disk as-is -- it is <em>not</em> fed back through the task
 * pipeline, so it gets no recursive carving, parsing, hashing, or
 * classification of its own. This mirrors what a full case would do only for
 * the first extraction step; nested content inside a carved file (e.g. a
 * carved zip containing further carvable files) is not expanded.
 */
public class CarvingOutputWorker extends AdditionalTaskWorker {

    private final File childrenDir;
    private final AtomicInteger counter = new AtomicInteger();

    public CarvingOutputWorker(int id, File output, File childrenDir) {
        super(id, output);
        this.childrenDir = childrenDir;
    }

    @Override
    public void processNewItem(IItem evidence) {
        try {
            if (!childrenDir.isDirectory() && !childrenDir.mkdirs()) {
                throw new IOException("Could not create output directory: " + childrenDir);
            }
            int n = counter.incrementAndGet();
            String baseName = String.format("%04d_%s", n, sanitize(evidence.getName()));
            File target = new File(childrenDir, baseName);

            try (InputStream is = evidence.getBufferedInputStream()) {
                if (is != null) {
                    Files.copy(is, target.toPath(), StandardCopyOption.REPLACE_EXISTING);
                } else {
                    Files.write(target.toPath(), new byte[0]);
                }
            }

            File sidecar = new File(childrenDir, baseName + ".metadata.txt");
            String metadata = "name: " + evidence.getName() + System.lineSeparator()
                    + "parentPath: " + evidence.getPath() + System.lineSeparator()
                    + "fileOffset: " + evidence.getFileOffset() + System.lineSeparator()
                    + "length: " + evidence.getLength() + System.lineSeparator()
                    + "mediaType: " + evidence.getMediaType() + System.lineSeparator();
            Files.writeString(sidecar.toPath(), metadata, StandardCharsets.UTF_8);

        } catch (IOException e) {
            throw new RuntimeException("Failed to write carved child '" + evidence.getName()
                    + "' to output directory " + childrenDir, e);
        }
    }

    private static String sanitize(String name) {
        return name == null ? "unnamed" : name.replaceAll("[\\\\/:*?\"<>|]", "_");
    }
}
