package iped.engine.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import iped.engine.config.ConfigurationView;
import iped.engine.data.CaseData;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** Tests to verify ProcessingOrchestrator manages multi-case processing. */
public class ProcessingOrchestratorTest {

  @BeforeEach
  void setUp() {
    // Initialize a fresh orchestrator for each test
    ProcessingOrchestrator.initialize(2, 1000000);
  }

  @AfterEach
  void tearDown() {
    CaseContextThreadLocal.clear();
  }

  private ProcessingOrchestrator getOrchestrator() {
    return ProcessingOrchestrator.getInstance();
  }

  @Test
  void testOrchestratorInstanceExists() {
    ProcessingOrchestrator orchestrator = getOrchestrator();
    assertNotNull(orchestrator, "Orchestrator should exist");
  }

  @Test
  void testEnqueueCaseForProcessing() throws InterruptedException {
    ProcessingOrchestrator orchestrator = getOrchestrator();
    UUID caseId = UUID.randomUUID();
    CaseContext context =
        new CaseContext.Builder(caseId)
            .withCaseData(new CaseData())
            .withConfigurationView(new ConfigurationView())
            .build();

    UUID returnedId = orchestrator.enqueueCaseForProcessing(context);
    assertEquals(caseId, returnedId, "Should return same case ID");
  }

  @Test
  void testResourceManagerAccess() {
    ProcessingOrchestrator orchestrator = getOrchestrator();
    ResourceManager resourceMgr = orchestrator.getResourceManager();
    assertNotNull(resourceMgr, "Resource manager should be accessible");
    assertEquals(2, resourceMgr.getMaxConcurrentCases());
    assertEquals(1000000, resourceMgr.getMaxMemoryPerCase());
  }
}
