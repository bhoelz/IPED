package iped.tasks.cli;

import iped.data.IItem;
import iped.engine.data.DataSource;
import iped.engine.data.Item;
import iped.utils.FileInputStreamFactory;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import org.apache.tika.mime.MediaType;

/**
 * Builds bare {@link IItem}s from a file or directory on disk, without a case or {@code
 * IPEDSource}. Reuses {@link FileInputStreamFactory} -- the same factory {@code
 * iped.engine.datasource.FolderTreeReader} wires up for folder-as-evidence processing -- so tasks
 * reading item content via {@code IItem#getSeekableInputStream()}/{@code getBufferedInputStream()}
 * work unmodified.
 */
public final class StandaloneItemFactory {

  private StandaloneItemFactory() {}

  public static List<IItem> fromInput(File input, boolean recursive) throws IOException {
    if (!input.exists()) {
      throw new IllegalArgumentException("Input not found: " + input);
    }
    if (input.isFile()) {
      // Mirror FolderTreeReader's representation of a file passed as the
      // evidence root: the factory root is the file itself and its
      // relative id is empty. This lets FileInputStreamFactory resolve
      // image-oriented access paths (getImageInputStream) correctly.
      Path root = input.getAbsoluteFile().toPath();
      List<IItem> items = new ArrayList<>(1);
      items.add(fromFile(input, root, ""));
      return items;
    }
    return fromDirectory(input, recursive);
  }

  private static List<IItem> fromDirectory(File dir, boolean recursive) throws IOException {
    List<IItem> items = new ArrayList<>();
    Path root = dir.getAbsoluteFile().toPath();
    int maxDepth = recursive ? Integer.MAX_VALUE : 1;
    try (var stream = Files.walk(root, maxDepth)) {
      for (Path path : (Iterable<Path>) stream::iterator) {
        File file = path.toFile();
        if (file.isFile()) {
          items.add(fromFile(file, root, root.relativize(path).toString()));
        }
      }
    }
    return items;
  }

  private static IItem fromFile(File file, Path root, String idInDataSource) throws IOException {
    Item item = new Item();
    item.setDataSource(new DataSource(root.toFile()));
    item.setInputStreamFactory(new FileInputStreamFactory(root));
    item.setIdInDataSource(idInDataSource);
    item.setPath(file.getAbsolutePath());
    item.setName(file.getName());
    item.setIsDir(false);
    item.setLength(file.length());
    // Standalone execution skips the pipeline's MIME-detection stage.
    // Tasks such as CarverTask still require a non-null type to decide
    // whether an input should be scanned, so use the neutral base type.
    item.setMediaType(MediaType.OCTET_STREAM);

    BasicFileAttributes attrs = Files.readAttributes(file.toPath(), BasicFileAttributes.class);
    item.setCreationDate(new Date(attrs.creationTime().toMillis()));
    item.setModificationDate(new Date(attrs.lastModifiedTime().toMillis()));
    item.setAccessDate(new Date(attrs.lastAccessTime().toMillis()));

    return item;
  }
}
