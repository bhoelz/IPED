package iped.distributed.resource;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;
import lombok.extern.slf4j.Slf4j;

/**
 * Samples an agent's local resource state — JVM heap, free disk on the work/output volume, and the
 * engine's {@link ResourceManager} pause state — and maps it to a {@link ResourcePressure} under a
 * {@link ResourcePressurePolicy}.
 *
 * <p>This is how a Task Agent produces the backpressure signal it reports to the coordinator on
 * each heartbeat: when the level is {@link PressureLevel#HARD} the coordinator withholds the
 * agent's free slots from scheduling and the agent pauses its own Kafka intake until pressure
 * clears (in-flight items always finish).
 *
 * <h2>Testability</h2>
 *
 * <p>The raw samplers are injected, so the threshold logic can be tested with synthetic heap/disk
 * values and no real JVM/disk dependency. {@link #forAgent} wires the default live samplers (JVM
 * {@link Runtime}, {@link Files#getFileStore}) plus an optional {@link ResourceManager}.
 *
 * <p>The monitor is stateless apart from its suppliers and is safe to sample from the heartbeat
 * thread.
 */
@Slf4j
public class ResourcePressureMonitor {

  /** One heap sample: bytes used and the maximum heap. */
  public record HeapSample(long usedBytes, long maxBytes) {}

  /** One disk sample: free and total bytes on the monitored volume. */
  public record DiskSample(long freeBytes, long totalBytes) {}

  private final ResourcePressurePolicy policy;
  private final Supplier<HeapSample> heapSampler;
  private final Supplier<DiskSample> diskSampler;
  private final BooleanSupplier engineUnderPressure;

  public ResourcePressureMonitor(
      ResourcePressurePolicy policy,
      Supplier<HeapSample> heapSampler,
      Supplier<DiskSample> diskSampler,
      BooleanSupplier engineUnderPressure) {
    this.policy = policy;
    this.heapSampler = heapSampler;
    this.diskSampler = diskSampler;
    this.engineUnderPressure = engineUnderPressure != null ? engineUnderPressure : () -> false;
  }

  /**
   * Builds a monitor with live samplers for a real agent.
   *
   * @param policy thresholds
   * @param workDir a path on the volume whose free space should be monitored (the shared output /
   *     temp directory)
   * @param enginePressureCheck optional supplier that returns {@code true} when the engine's {@code
   *     ResourceManager} has paused a case for memory reasons; {@code null} to ignore. Callers with
   *     access to the engine pass {@code resourceManager::enforceQuotas}.
   */
  public static ResourcePressureMonitor forAgent(
      ResourcePressurePolicy policy, Path workDir, BooleanSupplier enginePressureCheck) {
    Supplier<HeapSample> heap =
        () -> {
          Runtime rt = Runtime.getRuntime();
          long max = rt.maxMemory();
          long used = rt.totalMemory() - rt.freeMemory();
          return new HeapSample(used, max);
        };
    Supplier<DiskSample> disk =
        () -> {
          try {
            var store = Files.getFileStore(workDir);
            return new DiskSample(store.getUsableSpace(), store.getTotalSpace());
          } catch (IOException e) {
            log.debug("Could not sample disk for {}: {}", workDir, e.getMessage());
            return new DiskSample(Long.MAX_VALUE, Long.MAX_VALUE); // unknown → no disk pressure
          }
        };
    return new ResourcePressureMonitor(policy, heap, disk, enginePressureCheck);
  }

  /** Samples the current resource state and computes the resulting pressure. */
  public ResourcePressure sample() {
    HeapSample heap = heapSampler.get();
    DiskSample disk = diskSampler.get();
    boolean enginePressure = engineUnderPressure.getAsBoolean();

    double heapRatio = heap.maxBytes() > 0 ? (double) heap.usedBytes() / heap.maxBytes() : 0.0;

    PressureLevel level = PressureLevel.NONE;
    String reason = "ok";

    // Heap
    if (heapRatio >= policy.heapHardRatio()) {
      level = PressureLevel.max(level, PressureLevel.HARD);
      reason =
          String.format(
              "heap %.0f%% >= hard %.0f%%", heapRatio * 100, policy.heapHardRatio() * 100);
    } else if (heapRatio >= policy.heapSoftRatio()) {
      level = PressureLevel.max(level, PressureLevel.SOFT);
      reason =
          String.format(
              "heap %.0f%% >= soft %.0f%%", heapRatio * 100, policy.heapSoftRatio() * 100);
    }

    // Disk
    if (disk.freeBytes() <= policy.diskHardFreeBytes()) {
      level = PressureLevel.max(level, PressureLevel.HARD);
      reason = "free disk " + disk.freeBytes() + "B <= hard " + policy.diskHardFreeBytes() + "B";
    } else if (disk.freeBytes() <= policy.diskSoftFreeBytes()
        && !level.atLeast(PressureLevel.HARD)) {
      level = PressureLevel.max(level, PressureLevel.SOFT);
      if (level == PressureLevel.SOFT) {
        reason = "free disk " + disk.freeBytes() + "B <= soft " + policy.diskSoftFreeBytes() + "B";
      }
    }

    // Engine ResourceManager
    if (enginePressure) {
      level = PressureLevel.max(level, PressureLevel.HARD);
      if (level == PressureLevel.HARD && !reason.contains("disk") && !reason.contains("heap")) {
        reason = "engine ResourceManager paused a case (memory quota)";
      }
    }

    return new ResourcePressure(
        level,
        heapRatio,
        heap.usedBytes(),
        heap.maxBytes(),
        disk.freeBytes(),
        disk.totalBytes(),
        enginePressure,
        reason);
  }
}
