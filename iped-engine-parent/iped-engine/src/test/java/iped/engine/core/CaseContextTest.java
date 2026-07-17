package iped.engine.core;

import static org.junit.jupiter.api.Assertions.*;

import iped.engine.config.ConfigurationView;
import iped.engine.data.CaseData;
import java.util.UUID;
import org.junit.jupiter.api.Test;

public class CaseContextTest {

  @Test
  void testBuilderWithRequiredFields() {
    UUID caseId = UUID.randomUUID();
    CaseData caseData = new CaseData();
    ConfigurationView configView = new ConfigurationView();

    CaseContext context =
        new CaseContext.Builder(caseId)
            .withCaseData(caseData)
            .withConfigurationView(configView)
            .build();

    assertEquals(caseId, context.getId());
    assertEquals(caseData, context.getCaseData());
    assertEquals(configView, context.getConfigurationView());
    assertNull(context.getManager());
    assertNull(context.getStatistics());
    assertEquals(CaseContext.CaseState.QUEUED, context.getState());
  }

  @Test
  void testMissingCaseDataThrows() {
    assertThrows(
        IllegalArgumentException.class,
        () -> {
          new CaseContext.Builder(UUID.randomUUID())
              .withConfigurationView(new ConfigurationView())
              .build();
        });
  }

  @Test
  void testMissingConfigurationViewThrows() {
    assertThrows(
        IllegalArgumentException.class,
        () -> {
          new CaseContext.Builder(UUID.randomUUID()).withCaseData(new CaseData()).build();
        });
  }

  @Test
  void testStateTransitions() {
    CaseContext context =
        new CaseContext.Builder(UUID.randomUUID())
            .withCaseData(new CaseData())
            .withConfigurationView(new ConfigurationView())
            .build();

    assertEquals(CaseContext.CaseState.QUEUED, context.getState());

    context.setState(CaseContext.CaseState.RUNNING);
    assertEquals(CaseContext.CaseState.RUNNING, context.getState());

    context.setState(CaseContext.CaseState.PAUSED);
    assertEquals(CaseContext.CaseState.PAUSED, context.getState());

    context.setState(CaseContext.CaseState.COMPLETED);
    assertEquals(CaseContext.CaseState.COMPLETED, context.getState());
  }

  @Test
  void testExceptionHandling() {
    CaseContext context =
        new CaseContext.Builder(UUID.randomUUID())
            .withCaseData(new CaseData())
            .withConfigurationView(new ConfigurationView())
            .build();

    assertNull(context.getException());

    Exception testException = new RuntimeException("Test error");
    context.setException(testException);
    assertEquals(testException, context.getException());
  }

  @Test
  void testGeneratedCaseId() {
    CaseContext context1 =
        new CaseContext.Builder(null)
            .withCaseData(new CaseData())
            .withConfigurationView(new ConfigurationView())
            .build();

    CaseContext context2 =
        new CaseContext.Builder(null)
            .withCaseData(new CaseData())
            .withConfigurationView(new ConfigurationView())
            .build();

    assertNotNull(context1.getId());
    assertNotNull(context2.getId());
    assertNotEquals(context1.getId(), context2.getId());
  }
}
