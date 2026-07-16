package iped.tasks.cli;

import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Exercises the public CLI options for signature detection and child extraction. */
class CarverTaskCliTest {

    private static final File RELEASE_DIR = new File("../../target/release/iped-4.4.0-SNAPSHOT");
    private static final File CARVING_PROFILE_DIR = new File(RELEASE_DIR, "profiles/blind");

    @Test
    void carvesEmbeddedJpegUsingPublicCliOptions() throws Exception {
        Path sourceFile = Files.createTempFile("iped-tasks-cli-carver", ".bin");
        Path childrenDir = Files.createTempDirectory("iped-tasks-cli-carved-children");
        Path resultFile = Files.createTempFile("iped-tasks-cli-carver-result", ".json");
        try {
            assertTrue(CARVING_PROFILE_DIR.isDirectory(), "expected bundled carving-enabled profile");

            byte[] jpeg = createJpeg();
            byte[] payload = new byte[16 + jpeg.length + 9];
            System.arraycopy(jpeg, 0, payload, 16, jpeg.length);
            Files.write(sourceFile, payload);

            int exitCode = StandaloneTaskCli.execute(new String[] {
                    "--conf", CARVING_PROFILE_DIR.getCanonicalPath(),
                    "--task", "iped.engine.task.carver.CarverTask",
                    "--input", sourceFile.toString(),
                    "--extract-children-to", childrenDir.toString(),
                    "--output-format", "json",
                    "--output-file", resultFile.toString()
            });

            String result = Files.readString(resultFile);
            assertEquals(0, exitCode, result);
            assertTrue(result.contains("\"status\" : \"OK\""), result);
            File[] files = childrenDir.toFile().listFiles();
            assertTrue(files != null && files.length >= 2,
                    () -> "expected a carved file and its sidecar, got "
                            + (files == null ? "no directory listing" : Arrays.toString(files)));

            List<File> contents = Arrays.stream(files).filter(file -> !file.getName().endsWith(".metadata.txt")).toList();
            List<File> sidecars = Arrays.stream(files).filter(file -> file.getName().endsWith(".metadata.txt")).toList();
            assertEquals(contents.size(), sidecars.size(), "each carved child must have a metadata sidecar");
            assertTrue(contents.stream().allMatch(file -> {
                try {
                    return Arrays.equals(jpeg, Files.readAllBytes(file.toPath()));
                } catch (Exception e) {
                    return false;
                }
            }), "carved content must be the embedded JPEG");
            for (File sidecar : sidecars) {
                String metadata = Files.readString(sidecar.toPath());
                assertTrue(metadata.contains("fileOffset: 16"));
                assertTrue(metadata.contains("length: " + jpeg.length));
            }
        } finally {
            Files.deleteIfExists(sourceFile);
            Files.deleteIfExists(resultFile);
            deleteRecursively(childrenDir);
        }
    }

    private static byte[] createJpeg() throws Exception {
        BufferedImage image = new BufferedImage(80, 80, BufferedImage.TYPE_INT_RGB);
        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                image.setRGB(x, y, ((x * 3) << 16) | ((y * 3) << 8) | ((x + y) * 2));
            }
        }
        try (ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            assertTrue(ImageIO.write(image, "jpg", output));
            byte[] jpeg = output.toByteArray();
            assertTrue(jpeg.length >= 1000, "fixture must meet CarverConfig's JPEG minimum length");
            return jpeg;
        }
    }

    private static void deleteRecursively(Path dir) throws Exception {
        try (var stream = Files.walk(dir)) {
            stream.sorted(java.util.Comparator.reverseOrder()).forEach(path -> {
                try {
                    Files.deleteIfExists(path);
                } catch (Exception ignored) {
                }
            });
        }
    }
}
