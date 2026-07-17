package org.arabidopsis.ahocorasick;

import java.nio.charset.StandardCharsets;
import java.util.Iterator;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.Warmup;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

/**
 * JMH benchmark comparing AhoCorasick multi-pattern search against Java regex.
 *
 * <p>Run via Maven: mvn -pl iped-carvers/iped-ahocorasick test -Dtest=AhoCorasickBenchmark#main
 *
 * <p>Or build a benchmark jar and run directly: mvn -pl iped-carvers/iped-ahocorasick package -P
 * benchmark java -jar target/benchmarks.jar
 */
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@org.openjdk.jmh.annotations.State(Scope.Benchmark)
@Warmup(iterations = 3, time = 1)
@Measurement(iterations = 5, time = 2)
@Fork(1)
public class AhoCorasickBenchmark {

  // Patterns representative of carver header/footer signatures (short byte sequences)
  private static final String[] PATTERNS = {
    "Christmas", "Cains", "Marley", "spectre", "Ebenezer",
    "double-ironed", "supernatural", "SPIRITS", "Ding", "Ali Baba"
  };

  // Simulated 1 MB forensic image chunk — repeated text to create realistic density
  private static final byte[] HAYSTACK = buildHaystack();

  private AhoCorasick tree;
  private Pattern regexPattern;

  private static byte[] buildHaystack() {
    // A Christmas Carol text repeated to fill ~1 MB, mimicking a disk chunk scan
    String base =
        "It was a dark and stormy night. The ghost of Christmas past haunted Ebenezer Scrooge. "
            + "Jacob Marley rattled his chains, while the spectre of Cains loomed. "
            + "\"Ding dong!\" rang the bell as Ali Baba opened the cave. "
            + "The supernatural double-ironed chains clanked loudly, and SPIRITS filled the air. ";
    StringBuilder sb = new StringBuilder(1 << 20);
    while (sb.length() < (1 << 20)) {
      sb.append(base);
    }
    return sb.toString().getBytes(StandardCharsets.UTF_8);
  }

  @Setup(Level.Trial)
  public void setup() throws Exception {
    tree = new AhoCorasick();
    for (String p : PATTERNS) {
      byte[] b = p.getBytes(StandardCharsets.UTF_8);
      tree.add(b, b);
    }
    tree.prepare();

    StringBuilder rx = new StringBuilder();
    for (String p : PATTERNS) {
      if (rx.length() > 0) rx.append("|");
      rx.append(Pattern.quote(p));
    }
    regexPattern = Pattern.compile(rx.toString());
  }

  @org.openjdk.jmh.annotations.Benchmark
  public int ahoCorasick() {
    int count = 0;
    Iterator<SearchResult> it = tree.search(HAYSTACK);
    while (it.hasNext()) {
      it.next();
      count++;
    }
    return count;
  }

  @org.openjdk.jmh.annotations.Benchmark
  public int ahoCorasickLengthAware() {
    // Exercise the length-aware continueSearch path with an explicit length bound
    int count = 0;
    SearchResult result =
        tree.continueSearch(new SearchResult(tree.root, HAYSTACK, 0, HAYSTACK.length));
    while (result != null) {
      count++;
      result = tree.continueSearch(result);
    }
    return count;
  }

  @org.openjdk.jmh.annotations.Benchmark
  public int javaRegex() {
    int count = 0;
    Matcher m = regexPattern.matcher(new String(HAYSTACK, StandardCharsets.UTF_8));
    while (m.find()) count++;
    return count;
  }

  /** Entry point for running outside of JUnit (no @Test annotation needed). */
  public static void main(String[] args) throws RunnerException {
    Options opt = new OptionsBuilder().include(AhoCorasickBenchmark.class.getSimpleName()).build();
    new Runner(opt).run();
  }
}
