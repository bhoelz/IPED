package iped.distributed.resource;

import iped.distributed.config.DistributedConfig;

/**
 * Thresholds that map raw resource samples to a {@link PressureLevel}.
 *
 * @param heapSoftRatio heap-used ratio at/above which pressure is {@link PressureLevel#SOFT}
 * @param heapHardRatio heap-used ratio at/above which pressure is {@link PressureLevel#HARD}
 * @param diskSoftFreeBytes free disk at/below which pressure is {@link PressureLevel#SOFT}
 * @param diskHardFreeBytes free disk at/below which pressure is {@link PressureLevel#HARD}
 */
public record ResourcePressurePolicy(
    double heapSoftRatio, double heapHardRatio, long diskSoftFreeBytes, long diskHardFreeBytes) {
  public ResourcePressurePolicy {
    if (!(heapSoftRatio > 0 && heapSoftRatio <= 1) || !(heapHardRatio > 0 && heapHardRatio <= 1))
      throw new IllegalArgumentException("heap ratios must be in (0, 1]");
    if (heapHardRatio < heapSoftRatio)
      throw new IllegalArgumentException("heapHardRatio must be >= heapSoftRatio");
    if (diskHardFreeBytes < 0 || diskSoftFreeBytes < 0)
      throw new IllegalArgumentException("disk thresholds must be >= 0");
    if (diskSoftFreeBytes < diskHardFreeBytes)
      throw new IllegalArgumentException("diskSoftFreeBytes must be >= diskHardFreeBytes");
  }

  /** Heap 75% soft / 90% hard; disk 10 GB soft / 5 GB hard free. */
  public static ResourcePressurePolicy defaults() {
    return new ResourcePressurePolicy(
        0.75, 0.90, 10L * 1024 * 1024 * 1024, 5L * 1024 * 1024 * 1024);
  }

  public static ResourcePressurePolicy fromConfig(DistributedConfig cfg) {
    return new ResourcePressurePolicy(
        cfg.getBackpressureHeapSoftRatio(),
        cfg.getBackpressureHeapHardRatio(),
        cfg.getBackpressureDiskSoftFreeBytes(),
        cfg.getBackpressureDiskHardFreeBytes());
  }
}
