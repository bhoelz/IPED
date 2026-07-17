package iped.runner.web;

import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Slf4j
public class BrowseController {

  record Entry(String name, String type, String path) {}

  record BrowseResponse(String path, String parent, List<Entry> entries) {}

  /**
   * Lists the contents of a directory for the client-side file browser. When {@code dir} is blank,
   * returns filesystem roots (drive letters on Windows).
   */
  @GetMapping("/browse")
  public BrowseResponse browse(@RequestParam(defaultValue = "") String dir) {
    if (dir.isBlank()) {
      var roots = new ArrayList<Entry>();
      for (File root : File.listRoots()) {
        roots.add(new Entry(root.getPath(), "dir", root.getPath()));
      }
      return new BrowseResponse("", null, roots);
    }

    var path = Path.of(dir);
    if (!Files.isDirectory(path)) {
      return new BrowseResponse(dir, parentOf(path), List.of());
    }

    var entries = new ArrayList<Entry>();
    try (var stream = Files.list(path)) {
      stream
          .sorted(
              (a, b) -> {
                int da = Files.isDirectory(a) ? 0 : 1;
                int db = Files.isDirectory(b) ? 0 : 1;
                if (da != db) return da - db;
                return a.getFileName().toString().compareToIgnoreCase(b.getFileName().toString());
              })
          .forEach(
              p -> {
                boolean isDir = Files.isDirectory(p);
                entries.add(
                    new Entry(p.getFileName().toString(), isDir ? "dir" : "file", p.toString()));
              });
    } catch (IOException e) {
      log.warn("Cannot list directory {}: {}", path, e.getMessage());
    }

    return new BrowseResponse(dir, parentOf(path), entries);
  }

  private String parentOf(Path p) {
    var par = p.getParent();
    return par != null ? par.toString() : null;
  }
}
