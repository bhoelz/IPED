package iped.distributed.resource;

/**
 * A point-in-time snapshot of an agent's resource state, produced by {@link
 * ResourcePressureMonitor#sample()}.
 *
 * @param level overall pressure level (the most severe of all signals)
 * @param heapRatio heap used / heap max, in [0, 1]
 * @param heapUsedBytes JVM heap currently used
 * @param heapMaxBytes JVM heap maximum
 * @param freeDiskBytes free space on the monitored work/output volume
 * @param totalDiskBytes total space on that volume
 * @param engineUnderPressure true when the engine's {@code ResourceManager} reports a case paused
 *     for resource reasons
 * @param reason short human-readable explanation of the dominant signal
 */
public record ResourcePressure(
    PressureLevel level,
    double heapRatio,
    long heapUsedBytes,
    long heapMaxBytes,
    long freeDiskBytes,
    long totalDiskBytes,
    boolean engineUnderPressure,
    String reason) {
  /** True when the agent can accept new work (i.e. not {@link PressureLevel#HARD}). */
  public boolean accepting() {
    return level != PressureLevel.HARD;
  }

  /** A healthy, no-pressure snapshot (used as a default before the first sample). */
  public static ResourcePressure none() {
    return new ResourcePressure(PressureLevel.NONE, 0.0, 0, 0, 0, 0, false, "ok");
  }
}
