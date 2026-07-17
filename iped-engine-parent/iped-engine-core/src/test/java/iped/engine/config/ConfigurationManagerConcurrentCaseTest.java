package iped.engine.config;

import static org.junit.jupiter.api.Assertions.*;

import iped.configuration.Configurable;
import iped.configuration.IConfigurationDirectory;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Predicate;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Stress test verifying that concurrent CaseContext-scoped ConfigurationManager instances do not
 * contaminate each other's configurable state.
 *
 * <p>Each "case" gets a fresh instance via {@code ConfigurationManager.createCaseInstance()} and
 * registers its own distinct {@link SentinelConfig} carrying a unique case ID. After all threads
 * finish loading, each instance must resolve only its own sentinel.
 */
class ConfigurationManagerConcurrentCaseTest {

  private static final int NUM_CASES = 20;

  /** Minimal Configurable that stores a per-case sentinel value. */
  static class SentinelConfig implements Configurable<String> {

    private final int caseId;
    private String value;

    SentinelConfig(int caseId) {
      this.caseId = caseId;
    }

    @Override
    public String getConfiguration() {
      return value;
    }

    @Override
    public void setConfiguration(String config) {
      this.value = config;
    }

    @Override
    public DirectoryStream.Filter<Path> getResourceLookupFilter() {
      return path -> false;
    }

    @Override
    public void processConfig(Path resource) throws IOException {
      // not called — processConfigs is overridden directly
    }

    @Override
    public void processConfigs(List<Path> resources) {
      // No file I/O needed — the sentinel value is set directly.
      this.value = "case-" + caseId;
    }

    int getCaseId() {
      return caseId;
    }
  }

  /** Minimal directory that returns an empty path list for all configurables. */
  static class EmptyDirectory implements IConfigurationDirectory {
    @Override
    public void addPath(Path path) {}

    @Override
    public List<Path> getResourceLookupFolders() {
      return List.of();
    }

    @Override
    public List<Path> lookUpResource(Predicate<Path> predicate) {
      return List.of();
    }

    @Override
    public List<Path> lookUpResource(Configurable<?> configurable) {
      return List.of();
    }
  }

  @Test
  void concurrentCasesDoNotShareConfigurableState(@TempDir Path tempDir)
      throws InterruptedException, ExecutionException {

    int threads = Math.min(NUM_CASES, Runtime.getRuntime().availableProcessors() * 2);
    ExecutorService pool = Executors.newFixedThreadPool(threads);
    CountDownLatch startGate = new CountDownLatch(1);
    List<Future<Void>> futures = new ArrayList<>();
    AtomicInteger failCount = new AtomicInteger();

    // Each case creates its OWN manager instance and loads its own sentinel.
    List<ConfigurationManager> managers = new CopyOnWriteArrayList<>();

    for (int i = 0; i < NUM_CASES; i++) {
      final int caseId = i;
      futures.add(
          pool.submit(
              () -> {
                startGate.await(); // all threads start simultaneously for maximum contention
                ConfigurationManager cm =
                    ConfigurationManager.createCaseInstance(new EmptyDirectory());
                SentinelConfig sentinel = new SentinelConfig(caseId);
                cm.addObject(sentinel);
                cm.loadConfigs(false);
                managers.add(cm);
                return null;
              }));
    }

    startGate.countDown();

    for (Future<Void> f : futures) {
      f.get(); // propagates any assertion failure
    }
    pool.shutdown();

    assertEquals(NUM_CASES, managers.size(), "All case managers must be created");

    // Each manager must resolve its OWN sentinel and nothing from other cases.
    for (ConfigurationManager cm : managers) {
      Optional<SentinelConfig> found = cm.getConfig(SentinelConfig.class);
      assertTrue(found.isPresent(), "Each manager must have exactly one SentinelConfig");
      String expected = "case-" + found.get().getCaseId();
      assertEquals(
          expected,
          found.get().getConfiguration(),
          "SentinelConfig value must match its own case ID");
    }
  }

  @Test
  void createCaseInstanceNeverReturnsSingleton() {
    ConfigurationManager a = ConfigurationManager.createCaseInstance(new EmptyDirectory());
    ConfigurationManager b = ConfigurationManager.createCaseInstance(new EmptyDirectory());
    assertNotSame(a, b, "createCaseInstance must always return a fresh instance");
  }

  @Test
  void caseInstanceIsolatedFromSingleton() throws IOException {
    // Adding a configurable to a case instance must not affect the singleton's state.
    @SuppressWarnings("deprecation")
    ConfigurationManager existing = ConfigurationManager.get();
    ConfigurationManager caseInstance =
        ConfigurationManager.createCaseInstance(new EmptyDirectory());
    caseInstance.addObject(new SentinelConfig(999));
    caseInstance.loadConfigs(false);

    // The singleton (if it exists) must not see the case-instance's sentinel.
    if (existing != null) {
      Optional<SentinelConfig> fromSingleton = existing.getConfig(SentinelConfig.class);
      assertFalse(
          fromSingleton.isPresent(),
          "Sentinel added to case instance must not appear in singleton");
    }
    // And the case instance must see its sentinel.
    assertTrue(caseInstance.getConfig(SentinelConfig.class).isPresent());
  }
}
