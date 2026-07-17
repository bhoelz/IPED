package iped.runner.execution;

import java.util.ArrayDeque;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Pattern;

/**
 * Mutable statistics for a single running IPED job, updated from stdout lines. Thread-safe: the I/O
 * thread writes via {@link #addLine}, any thread reads via accessors.
 */
class RunStats {

  // Matches patterns like "1 234 items processed" or "processed 1234 of 5678 items"
  private static final Pattern ITEMS_PROCESSED =
      Pattern.compile(
          "(?:processed|\\bprocessed)[\\s:]+([\\d,_.]+)(?:\\s+(?:of|/)\\s+([\\d,_.]+))?",
          Pattern.CASE_INSENSITIVE);

  // Matches patterns like "500 MB/s", "1.2 GB/s", "234 items/s"
  private static final Pattern SPEED =
      Pattern.compile(
          "(\\d+(?:[.,]\\d+)?\\s*[KMGT]?B/s|\\d+\\s+items?/s)", Pattern.CASE_INSENSITIVE);

  // Matches "ETA: 0h 3m 12s" or "ETA 3:12" etc.
  private static final Pattern ETA =
      Pattern.compile("\\bETA[:\\s]+([\\dh m s:]+)", Pattern.CASE_INSENSITIVE);

  volatile String status = "running";
  final AtomicLong itemsProcessed = new AtomicLong(0);
  final AtomicLong itemsFound = new AtomicLong(0);
  final AtomicReference<String> currentSpeed = new AtomicReference<>("—");
  final AtomicReference<String> eta = new AtomicReference<>("—");

  private final ArrayDeque<String> recentLines = new ArrayDeque<>(100);

  synchronized void addLine(String line) {
    if (recentLines.size() >= 100) recentLines.pollFirst();
    recentLines.addLast(line);
    parseLine(line);
  }

  synchronized List<String> tailLines(int n) {
    var all = List.copyOf(recentLines);
    return all.subList(Math.max(0, all.size() - n), all.size());
  }

  private void parseLine(String line) {
    var m = ITEMS_PROCESSED.matcher(line);
    if (m.find()) {
      itemsProcessed.set(parseLong(m.group(1)));
      if (m.group(2) != null) itemsFound.set(parseLong(m.group(2)));
    }
    m = SPEED.matcher(line);
    if (m.find()) currentSpeed.set(m.group(1).trim());
    m = ETA.matcher(line);
    if (m.find()) eta.set(m.group(1).trim());
  }

  private static long parseLong(String s) {
    try {
      return Long.parseLong(s.replaceAll("[,_. ]", ""));
    } catch (NumberFormatException e) {
      return 0;
    }
  }
}
