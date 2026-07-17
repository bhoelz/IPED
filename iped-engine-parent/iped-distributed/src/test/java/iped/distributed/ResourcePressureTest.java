package iped.distributed;

import static org.junit.jupiter.api.Assertions.*;

import iped.distributed.coordinator.AgentRegistration;
import iped.distributed.coordinator.AgentRegistry;
import iped.distributed.resource.PressureLevel;
import iped.distributed.resource.ResourcePressure;
import iped.distributed.resource.ResourcePressureMonitor;
import iped.distributed.resource.ResourcePressureMonitor.DiskSample;
import iped.distributed.resource.ResourcePressureMonitor.HeapSample;
import iped.distributed.resource.ResourcePressurePolicy;
import org.junit.jupiter.api.Test;

class ResourcePressureTest {

  // ── Policy validation ────────────────────────────────────────────────────

  @Test
  void policyDefaults() {
    ResourcePressurePolicy p = ResourcePressurePolicy.defaults();
    assertEquals(0.75, p.heapSoftRatio());
    assertEquals(0.90, p.heapHardRatio());
    assertEquals(10L * 1024 * 1024 * 1024, p.diskSoftFreeBytes());
    assertEquals(5L * 1024 * 1024 * 1024, p.diskHardFreeBytes());
  }

  @Test
  void policyRejectsHardLessThanSoft() {
    assertThrows(
        IllegalArgumentException.class,
        () -> new ResourcePressurePolicy(0.90, 0.75, 10_000, 5_000));
  }

  @Test
  void policyRejectsDiskSoftBelowHard() {
    assertThrows(
        IllegalArgumentException.class, () -> new ResourcePressurePolicy(0.75, 0.90, 4_000, 5_000));
  }

  // ── Heap threshold logic ─────────────────────────────────────────────────

  @Test
  void heapBelowSoftIsNone() {
    ResourcePressure p = monitorWithHeap(0.70, 0.75, 0.90).sample();
    assertEquals(PressureLevel.NONE, p.level());
    assertTrue(p.accepting());
  }

  @Test
  void heapAtSoftIsSoft() {
    ResourcePressure p = monitorWithHeap(0.75, 0.75, 0.90).sample();
    assertEquals(PressureLevel.SOFT, p.level());
    assertTrue(p.accepting());
  }

  @Test
  void heapBetweenSoftAndHardIsSoft() {
    ResourcePressure p = monitorWithHeap(0.82, 0.75, 0.90).sample();
    assertEquals(PressureLevel.SOFT, p.level());
  }

  @Test
  void heapAtHardIsHard() {
    ResourcePressure p = monitorWithHeap(0.90, 0.75, 0.90).sample();
    assertEquals(PressureLevel.HARD, p.level());
    assertFalse(p.accepting());
  }

  @Test
  void heapAboveHardIsHard() {
    ResourcePressure p = monitorWithHeap(0.97, 0.75, 0.90).sample();
    assertEquals(PressureLevel.HARD, p.level());
  }

  // ── Disk threshold logic ─────────────────────────────────────────────────

  @Test
  void diskAboveSoftIsNone() {
    ResourcePressure p = monitorWithDisk(20_000L, 10_000L, 5_000L).sample();
    assertEquals(PressureLevel.NONE, p.level());
  }

  @Test
  void diskAtSoftIsSoft() {
    ResourcePressure p = monitorWithDisk(10_000L, 10_000L, 5_000L).sample();
    assertEquals(PressureLevel.SOFT, p.level());
  }

  @Test
  void diskBetweenSoftAndHardIsSoft() {
    ResourcePressure p = monitorWithDisk(7_500L, 10_000L, 5_000L).sample();
    assertEquals(PressureLevel.SOFT, p.level());
  }

  @Test
  void diskAtHardIsHard() {
    ResourcePressure p = monitorWithDisk(5_000L, 10_000L, 5_000L).sample();
    assertEquals(PressureLevel.HARD, p.level());
    assertFalse(p.accepting());
  }

  @Test
  void diskBelowHardIsHard() {
    ResourcePressure p = monitorWithDisk(100L, 10_000L, 5_000L).sample();
    assertEquals(PressureLevel.HARD, p.level());
  }

  // ── Engine pressure ──────────────────────────────────────────────────────

  @Test
  void enginePressureAloneRaisesHard() {
    ResourcePressurePolicy policy = ResourcePressurePolicy.defaults();
    ResourcePressureMonitor monitor =
        new ResourcePressureMonitor(
            policy,
            () -> new HeapSample(70L, 100L), // 70% — below soft
            () -> new DiskSample(Long.MAX_VALUE, Long.MAX_VALUE), // no disk pressure
            () -> true // engine says paused
            );
    ResourcePressure p = monitor.sample();
    assertEquals(PressureLevel.HARD, p.level());
    assertTrue(p.engineUnderPressure());
  }

  @Test
  void enginePressureFalseLeavesLevelAlone() {
    ResourcePressurePolicy policy = ResourcePressurePolicy.defaults();
    ResourcePressureMonitor monitor =
        new ResourcePressureMonitor(
            policy,
            () -> new HeapSample(80L, 100L), // 80% — SOFT
            () -> new DiskSample(Long.MAX_VALUE, Long.MAX_VALUE),
            () -> false);
    ResourcePressure p = monitor.sample();
    assertEquals(PressureLevel.SOFT, p.level());
    assertFalse(p.engineUnderPressure());
  }

  // ── Combined signals: worst wins ─────────────────────────────────────────

  @Test
  void diskSoftHeapHardGivesHard() {
    ResourcePressurePolicy policy = new ResourcePressurePolicy(0.75, 0.90, 10_000L, 5_000L);
    ResourcePressureMonitor monitor =
        new ResourcePressureMonitor(
            policy,
            () -> new HeapSample(92L, 100L), // 92% — HARD
            () -> new DiskSample(7_500L, 20_000L), // soft only
            () -> false);
    assertEquals(PressureLevel.HARD, monitor.sample().level());
  }

  @Test
  void heapSoftDiskHardGivesHard() {
    ResourcePressurePolicy policy = new ResourcePressurePolicy(0.75, 0.90, 10_000L, 5_000L);
    ResourcePressureMonitor monitor =
        new ResourcePressureMonitor(
            policy,
            () -> new HeapSample(78L, 100L), // 78% — SOFT
            () -> new DiskSample(4_000L, 20_000L), // HARD
            () -> false);
    assertEquals(PressureLevel.HARD, monitor.sample().level());
  }

  // ── PressureLevel helpers ────────────────────────────────────────────────

  @Test
  void pressureLevelAtLeast() {
    assertTrue(PressureLevel.HARD.atLeast(PressureLevel.HARD));
    assertTrue(PressureLevel.HARD.atLeast(PressureLevel.SOFT));
    assertTrue(PressureLevel.HARD.atLeast(PressureLevel.NONE));
    assertTrue(PressureLevel.SOFT.atLeast(PressureLevel.NONE));
    assertFalse(PressureLevel.SOFT.atLeast(PressureLevel.HARD));
    assertFalse(PressureLevel.NONE.atLeast(PressureLevel.SOFT));
  }

  @Test
  void pressureLevelMax() {
    assertEquals(PressureLevel.HARD, PressureLevel.max(PressureLevel.HARD, PressureLevel.SOFT));
    assertEquals(PressureLevel.HARD, PressureLevel.max(PressureLevel.NONE, PressureLevel.HARD));
    assertEquals(PressureLevel.SOFT, PressureLevel.max(PressureLevel.SOFT, PressureLevel.NONE));
  }

  // ── AgentRegistry: HARD agents excluded from schedulable slots ───────────

  @Test
  void hardAgentExcludedFromSchedulableSlots() {
    AgentRegistry registry = new AgentRegistry(60);
    AgentRegistration agent = AgentRegistration.of("a1", "HashTask", 1, 4, "host1");
    registry.register(agent);

    // Initially schedulable (NONE pressure)
    assertEquals(4, registry.schedulableFreeSlots("HashTask"));
    assertEquals(0, registry.pressuredAgentCount("HashTask"));

    // Heartbeat with HARD pressure
    registry.heartbeat("a1", 4, 0, PressureLevel.HARD, 0.92);
    assertEquals(0, registry.schedulableFreeSlots("HashTask"));
    assertEquals(1, registry.pressuredAgentCount("HashTask"));

    // Heartbeat clears to SOFT — should be schedulable again
    registry.heartbeat("a1", 4, 0, PressureLevel.SOFT, 0.80);
    assertEquals(4, registry.schedulableFreeSlots("HashTask"));
    assertEquals(0, registry.pressuredAgentCount("HashTask"));
  }

  @Test
  void softAgentStillSchedulable() {
    AgentRegistry registry = new AgentRegistry(60);
    AgentRegistration agent = AgentRegistration.of("a2", "OcrTask", 2, 2, "host2");
    registry.register(agent);

    registry.heartbeat("a2", 2, 0, PressureLevel.SOFT, 0.80);
    assertEquals(2, registry.schedulableFreeSlots("OcrTask"));
  }

  @Test
  void mixedPressureAgentsPartialSlots() {
    AgentRegistry registry = new AgentRegistry(60);
    registry.register(AgentRegistration.of("a1", "HashTask", 1, 4, "h1"));
    registry.register(AgentRegistration.of("a2", "HashTask", 1, 4, "h2"));

    // a1 HARD, a2 NONE → only a2's slots are schedulable
    registry.heartbeat("a1", 4, 0, PressureLevel.HARD, 0.91);
    registry.heartbeat("a2", 3, 1, PressureLevel.NONE, 0.40);

    assertEquals(3, registry.schedulableFreeSlots("HashTask"));
    assertEquals(1, registry.pressuredAgentCount("HashTask"));
  }

  // ── ResourcePressure record helpers ─────────────────────────────────────

  @Test
  void noneSnapshotIsAccepting() {
    ResourcePressure none = ResourcePressure.none();
    assertEquals(PressureLevel.NONE, none.level());
    assertTrue(none.accepting());
  }

  // ── Helpers ──────────────────────────────────────────────────────────────

  /** Build a monitor with synthetic heap (ratio) and no disk/engine pressure. */
  private static ResourcePressureMonitor monitorWithHeap(
      double heapRatio, double softThreshold, double hardThreshold) {
    ResourcePressurePolicy policy =
        new ResourcePressurePolicy(softThreshold, hardThreshold, 10_000L, 5_000L);
    long max = 1_000L;
    long used = (long) (heapRatio * max);
    return new ResourcePressureMonitor(
        policy,
        () -> new HeapSample(used, max),
        () -> new DiskSample(Long.MAX_VALUE, Long.MAX_VALUE),
        () -> false);
  }

  /** Build a monitor with synthetic disk and no heap/engine pressure. */
  private static ResourcePressureMonitor monitorWithDisk(
      long freeBytes, long softThreshold, long hardThreshold) {
    ResourcePressurePolicy policy =
        new ResourcePressurePolicy(0.75, 0.90, softThreshold, hardThreshold);
    return new ResourcePressureMonitor(
        policy,
        () -> new HeapSample(10L, 1_000L), // 1% heap — no pressure
        () -> new DiskSample(freeBytes, 1_000_000L),
        () -> false);
  }
}
