package iped.distributed;

import iped.distributed.config.DistributedConfig;
import iped.distributed.kafka.KafkaItemMessage;
import iped.distributed.workunit.AdaptiveWorkUnitPlanner;
import iped.distributed.workunit.MediaCostModel;
import iped.distributed.workunit.WorkUnit;
import iped.distributed.workunit.WorkUnitSizingPolicy;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for adaptive work-unit sizing: {@link AdaptiveWorkUnitPlanner},
 * {@link WorkUnitSizingPolicy}, and {@link MediaCostModel}.
 *
 * <p>Pure logic — no Kafka broker or coordinator required.
 */
class AdaptiveWorkUnitPlannerTest {

    private static final long KB = 1024;
    private static final long MB = 1024 * 1024;

    // ---- item builders -----------------------------------------------------

    private static KafkaItemMessage item(long length, String mediaType, String ext) {
        KafkaItemMessage m = new KafkaItemMessage();
        m.setItemUuid(UUID.randomUUID().toString());
        m.setLength(length);
        m.setMediaType(mediaType);
        m.setExtension(ext);
        return m;
    }

    private static KafkaItemMessage plainFile(long length) {
        return item(length, "text/plain", "txt");
    }

    private static List<KafkaItemMessage> nPlainFiles(int n, long eachBytes) {
        List<KafkaItemMessage> list = new ArrayList<>();
        for (int i = 0; i < n; i++) list.add(plainFile(eachBytes));
        return list;
    }

    // =========================================================================
    // Policy validation
    // =========================================================================

    @Test
    void policyRejectsNonPositiveTarget() {
        assertThrows(IllegalArgumentException.class,
                () -> new WorkUnitSizingPolicy(0, 10, 100));
    }

    @Test
    void policyRejectsNonPositiveMaxItems() {
        assertThrows(IllegalArgumentException.class,
                () -> new WorkUnitSizingPolicy(100, 0, 100));
    }

    @Test
    void policyRejectsOversizedBelowTarget() {
        assertThrows(IllegalArgumentException.class,
                () -> new WorkUnitSizingPolicy(100, 10, 50));
    }

    @Test
    void policyFromConfigUsesConfiguredValues() {
        DistributedConfig cfg = new DistributedConfig();
        WorkUnitSizingPolicy p = WorkUnitSizingPolicy.fromConfig(cfg);
        assertEquals(cfg.getWorkUnitTargetBytes(), p.targetUnitBytes());
        assertEquals(cfg.getWorkUnitMaxItems(), p.maxUnitItems());
        assertEquals(cfg.getWorkUnitOversizedBytes(), p.oversizedItemBytes());
    }

    // =========================================================================
    // Empty / null handling
    // =========================================================================

    @Test
    void emptyInputProducesNoUnits() {
        AdaptiveWorkUnitPlanner planner = new AdaptiveWorkUnitPlanner();
        assertTrue(planner.plan(List.of()).isEmpty());
        assertTrue(planner.plan(null).isEmpty());
    }

    @Test
    void nullItemsAreSkipped() {
        AdaptiveWorkUnitPlanner planner = new AdaptiveWorkUnitPlanner();
        List<KafkaItemMessage> items = new ArrayList<>();
        items.add(plainFile(1 * KB));
        items.add(null);
        items.add(plainFile(1 * KB));
        List<WorkUnit> units = planner.plan(items);
        int total = units.stream().mapToInt(WorkUnit::itemCount).sum();
        assertEquals(2, total, "null item must be skipped, the two real items kept");
    }

    // =========================================================================
    // Batching small items together (the core win over one-unit-per-item)
    // =========================================================================

    @Test
    void manyTinyFilesBatchByItemCap() {
        // 1000 files of 1 KB each → byte target (64 MB) never reached, so the
        // 256-item cap governs: ceil(1000/256) = 4 units.
        AdaptiveWorkUnitPlanner planner = new AdaptiveWorkUnitPlanner();
        List<WorkUnit> units = planner.plan(nPlainFiles(1000, 1 * KB));
        assertEquals(4, units.size());
        assertEquals(256, units.get(0).itemCount());
        assertEquals(256, units.get(1).itemCount());
        assertEquals(256, units.get(2).itemCount());
        assertEquals(1000 - 768, units.get(3).itemCount()); // 232
    }

    @Test
    void smallFilesBatchByByteTarget() {
        // 10 MB plain files, target 64 MB → 6 fit (60 MB) before the 7th would exceed.
        WorkUnitSizingPolicy policy = new WorkUnitSizingPolicy(64 * MB, 1000, 128 * MB);
        AdaptiveWorkUnitPlanner planner = new AdaptiveWorkUnitPlanner(policy, new MediaCostModel());
        List<WorkUnit> units = planner.plan(nPlainFiles(13, 10 * MB));
        // 13 files: 6 + 6 + 1
        assertEquals(3, units.size());
        assertEquals(6, units.get(0).itemCount());
        assertEquals(6, units.get(1).itemCount());
        assertEquals(1, units.get(2).itemCount());
    }

    @Test
    void unitIndicesAreSequential() {
        AdaptiveWorkUnitPlanner planner = new AdaptiveWorkUnitPlanner();
        List<WorkUnit> units = planner.plan(nPlainFiles(600, 1 * KB));
        for (int i = 0; i < units.size(); i++) {
            assertEquals(i, units.get(i).index(), "unit index must equal its position");
        }
    }

    @Test
    void allItemsPreservedAcrossUnits() {
        AdaptiveWorkUnitPlanner planner = new AdaptiveWorkUnitPlanner();
        List<KafkaItemMessage> items = nPlainFiles(500, 2 * KB);
        List<WorkUnit> units = planner.plan(items);

        List<String> planned = new ArrayList<>();
        units.forEach(u -> planned.addAll(u.itemUuids()));
        List<String> expected = items.stream().map(KafkaItemMessage::getItemUuid).toList();
        assertEquals(expected, planned, "every item appears exactly once, in input order");
    }

    // =========================================================================
    // Oversized isolation
    // =========================================================================

    @Test
    void oversizedItemIsolatedIntoOwnUnit() {
        WorkUnitSizingPolicy policy = new WorkUnitSizingPolicy(64 * MB, 256, 128 * MB);
        AdaptiveWorkUnitPlanner planner = new AdaptiveWorkUnitPlanner(policy, new MediaCostModel());

        List<KafkaItemMessage> items = new ArrayList<>();
        items.add(plainFile(1 * KB));
        items.add(plainFile(200 * MB)); // weighted 200 MB ≥ 128 MB threshold → isolated
        items.add(plainFile(1 * KB));

        List<WorkUnit> units = planner.plan(items);
        assertEquals(3, units.size(), "small / oversized / small → three units");
        assertFalse(units.get(0).oversized());
        assertTrue(units.get(1).oversized(), "the 200 MB item must be flagged oversized");
        assertEquals(1, units.get(1).itemCount());
        assertFalse(units.get(2).oversized());
    }

    @Test
    void oversizedItemFlushesPendingUnitFirst() {
        WorkUnitSizingPolicy policy = new WorkUnitSizingPolicy(64 * MB, 256, 128 * MB);
        AdaptiveWorkUnitPlanner planner = new AdaptiveWorkUnitPlanner(policy, new MediaCostModel());

        List<KafkaItemMessage> items = new ArrayList<>();
        items.add(plainFile(1 * KB));
        items.add(plainFile(1 * KB));
        items.add(plainFile(200 * MB)); // isolate, but flush the 2 pending first

        List<WorkUnit> units = planner.plan(items);
        assertEquals(2, units.size());
        assertEquals(2, units.get(0).itemCount(), "pending small items flushed as their own unit");
        assertTrue(units.get(1).oversized());
    }

    @Test
    void exactlyAtOversizedThresholdIsIsolated() {
        // weighted size == threshold counts as oversized (>=)
        WorkUnitSizingPolicy policy = new WorkUnitSizingPolicy(64 * MB, 256, 128 * MB);
        AdaptiveWorkUnitPlanner planner = new AdaptiveWorkUnitPlanner(policy, new MediaCostModel());
        List<WorkUnit> units = planner.plan(List.of(plainFile(128 * MB)));
        assertEquals(1, units.size());
        assertTrue(units.get(0).oversized());
    }

    // =========================================================================
    // Media-type weighting drives smaller batches for expensive types
    // =========================================================================

    @Test
    void expensiveTypesFillUnitFasterThanCheapTypes() {
        WorkUnitSizingPolicy policy = new WorkUnitSizingPolicy(60 * MB, 1000, 200 * MB);
        AdaptiveWorkUnitPlanner planner = new AdaptiveWorkUnitPlanner(policy, new MediaCostModel());

        // Videos weigh 4× → a 10 MB video counts as 40 MB weighted.
        // Two videos = 80 MB weighted > 60 MB target → they split across 2 units.
        List<KafkaItemMessage> videos = List.of(
                item(10 * MB, "video/mp4", "mp4"),
                item(10 * MB, "video/mp4", "mp4"));
        List<WorkUnit> videoUnits = planner.plan(videos);
        assertEquals(2, videoUnits.size(), "two 10 MB videos exceed target when weighted 4×");

        // Two 10 MB plain files = 20 MB weighted < 60 MB → one unit.
        List<WorkUnit> textUnits = planner.plan(nPlainFiles(2, 10 * MB));
        assertEquals(1, textUnits.size(), "two 10 MB text files fit comfortably in one unit");
    }

    @Test
    void weightedBytesReflectsCostModel() {
        AdaptiveWorkUnitPlanner planner = new AdaptiveWorkUnitPlanner();
        // single 1 MB video → weighted 4 MB
        List<WorkUnit> units = planner.plan(List.of(item(1 * MB, "video/mp4", "mp4")));
        assertEquals(1, units.size());
        assertEquals(1 * MB, units.get(0).totalBytes(), "raw bytes unweighted");
        assertEquals(4.0 * MB, units.get(0).weightedBytes(), 0.001, "weighted 4× for video");
    }

    // =========================================================================
    // MediaCostModel directly
    // =========================================================================

    @Test
    void costModelWeightsByType() {
        MediaCostModel m = new MediaCostModel();
        assertEquals(4.0, m.weight(item(1 * MB, "video/mp4", "mp4")), 0.0001);
        assertEquals(3.0, m.weight(item(1 * MB, "application/zip", "zip")), 0.0001);
        assertEquals(1.5, m.weight(item(1 * MB, "image/jpeg", "jpg")), 0.0001);
        assertEquals(1.0, m.weight(item(1 * MB, "text/plain", "txt")), 0.0001);
    }

    @Test
    void costModelFallsBackToExtensionWhenTypeMissing() {
        MediaCostModel m = new MediaCostModel();
        assertEquals(4.0, m.weight(item(1 * MB, null, "mkv")), 0.0001,
                "no media type → use extension");
        assertEquals(3.0, m.weight(item(1 * MB, null, "rar")), 0.0001);
    }

    @Test
    void costModelTreatsDirectoriesAndEmptyAsCheap() {
        MediaCostModel m = new MediaCostModel();
        KafkaItemMessage dir = item(0, "inode/directory", null);
        dir.setDir(true);
        assertEquals(MediaCostModel.CHEAP_WEIGHT, m.weight(dir), 0.0001);

        KafkaItemMessage empty = item(0, "text/plain", "txt");
        assertEquals(MediaCostModel.CHEAP_WEIGHT, m.weight(empty), 0.0001,
                "zero-length item is cheap regardless of type");
    }

    @Test
    void costModelHandlesNullAndUnknown() {
        MediaCostModel m = new MediaCostModel();
        assertEquals(MediaCostModel.DEFAULT_WEIGHT, m.weight(null), 0.0001);
        assertEquals(MediaCostModel.DEFAULT_WEIGHT,
                m.weight(item(1 * MB, "application/x-unknown-thing", "qqq")), 0.0001);
    }

    @Test
    void costModelStripsMediaTypeParameters() {
        MediaCostModel m = new MediaCostModel();
        assertEquals(1.0, m.weight(item(1 * MB, "text/plain; charset=utf-8", "txt")), 0.0001,
                "parameters after ';' must be ignored");
    }

    // =========================================================================
    // Determinism
    // =========================================================================

    @Test
    void planIsDeterministicForSameInput() {
        AdaptiveWorkUnitPlanner planner = new AdaptiveWorkUnitPlanner();
        List<KafkaItemMessage> items = new ArrayList<>();
        for (int i = 0; i < 300; i++) items.add(plainFile((i % 7 + 1) * KB));

        List<WorkUnit> a = planner.plan(items);
        List<WorkUnit> b = planner.plan(items);
        assertEquals(a.size(), b.size());
        for (int i = 0; i < a.size(); i++) {
            assertEquals(a.get(i).itemUuids(), b.get(i).itemUuids(),
                    "same input must yield identical unit membership");
        }
    }

    // =========================================================================
    // Mixed realistic workload
    // =========================================================================

    @Test
    void mixedWorkloadSeparatesHugeFromSmall() {
        WorkUnitSizingPolicy policy = new WorkUnitSizingPolicy(64 * MB, 256, 128 * MB);
        AdaptiveWorkUnitPlanner planner = new AdaptiveWorkUnitPlanner(policy, new MediaCostModel());

        List<KafkaItemMessage> items = new ArrayList<>();
        items.addAll(nPlainFiles(500, 4 * KB));            // ~2 MB of tiny files
        items.add(item(2L * 1024 * MB, null, "e01"));      // 2 GB disk image → oversized
        items.addAll(nPlainFiles(100, 4 * KB));

        List<WorkUnit> units = planner.plan(items);

        long oversizedUnits = units.stream().filter(WorkUnit::oversized).count();
        assertEquals(1, oversizedUnits, "exactly the disk image is isolated");

        // Every non-oversized unit respects the item cap.
        units.stream().filter(u -> !u.oversized())
                .forEach(u -> assertTrue(u.itemCount() <= 256));

        int totalItems = units.stream().mapToInt(WorkUnit::itemCount).sum();
        assertEquals(601, totalItems, "no item lost");
    }
}
