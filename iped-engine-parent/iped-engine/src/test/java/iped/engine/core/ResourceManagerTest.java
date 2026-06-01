package iped.engine.core;

import static org.junit.jupiter.api.Assertions.*;

import java.util.UUID;

import org.junit.jupiter.api.Test;

/**
 * Tests to verify ResourceManager enforces quotas and tracks resources.
 */
public class ResourceManagerTest {

    @Test
    void testAcquireResourcesSucceeds() {
        ResourceManager manager = new ResourceManager(1000000, 2);
        UUID caseId = UUID.randomUUID();

        manager.acquireResources(caseId);
        assertEquals(1, manager.getActiveCaseCount(), "Should have one active case");
    }

    @Test
    void testMultipleCasesAcquireResources() {
        ResourceManager manager = new ResourceManager(1000000, 3);
        UUID case1Id = UUID.randomUUID();
        UUID case2Id = UUID.randomUUID();

        manager.acquireResources(case1Id);
        manager.acquireResources(case2Id);

        assertEquals(2, manager.getActiveCaseCount(), "Should have two active cases");
    }

    @Test
    void testAcquireResourcesFailsWhenLimitExceeded() {
        ResourceManager manager = new ResourceManager(1000000, 1);
        UUID case1Id = UUID.randomUUID();
        UUID case2Id = UUID.randomUUID();

        manager.acquireResources(case1Id);

        assertThrows(IllegalStateException.class, () -> manager.acquireResources(case2Id),
                "Should throw when max concurrent cases exceeded");
    }

    @Test
    void testReleaseResources() {
        ResourceManager manager = new ResourceManager(1000000, 2);
        UUID caseId = UUID.randomUUID();

        manager.acquireResources(caseId);
        assertEquals(1, manager.getActiveCaseCount());

        manager.releaseResources(caseId);
        assertEquals(0, manager.getActiveCaseCount(), "Should have no active cases after release");
    }

    @Test
    void testMemoryUsageTracking() {
        ResourceManager manager = new ResourceManager(1000000, 2);
        UUID caseId = UUID.randomUUID();

        manager.acquireResources(caseId);
        assertEquals(0, manager.getMemoryUsage(caseId), "Should start with 0 memory");

        manager.updateMemoryUsage(caseId, 500000);
        assertEquals(500000, manager.getMemoryUsage(caseId), "Should track memory correctly");
    }

    @Test
    void testMemoryQuotaEnforcement() {
        ResourceManager manager = new ResourceManager(500000, 2);
        UUID caseId = UUID.randomUUID();

        manager.acquireResources(caseId);
        assertFalse(manager.isPaused(caseId), "Should not be paused initially");

        manager.updateMemoryUsage(caseId, 600000);
        manager.enforceQuotas();

        assertTrue(manager.isPaused(caseId), "Should be paused when exceeding quota");
    }

    @Test
    void testMemoryQuotaResume() {
        ResourceManager manager = new ResourceManager(500000, 2);
        UUID caseId = UUID.randomUUID();

        manager.acquireResources(caseId);
        manager.updateMemoryUsage(caseId, 600000);
        manager.enforceQuotas();
        assertTrue(manager.isPaused(caseId));

        manager.updateMemoryUsage(caseId, 400000);
        manager.enforceQuotas();
        assertFalse(manager.isPaused(caseId), "Should resume when memory drops below quota");
    }

    @Test
    void testTotalMemoryTracking() {
        ResourceManager manager = new ResourceManager(1000000, 3);
        UUID case1Id = UUID.randomUUID();
        UUID case2Id = UUID.randomUUID();

        manager.acquireResources(case1Id);
        manager.acquireResources(case2Id);

        manager.updateMemoryUsage(case1Id, 300000);
        manager.updateMemoryUsage(case2Id, 200000);

        assertEquals(500000, manager.getTotalMemoryUsage(), "Should track total memory correctly");
    }

    @Test
    void testConfigurationValues() {
        ResourceManager manager = new ResourceManager(1000000, 2);

        assertEquals(1000000, manager.getMaxMemoryPerCase());
        assertEquals(2, manager.getMaxConcurrentCases());
    }
}
