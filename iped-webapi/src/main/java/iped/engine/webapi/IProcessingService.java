package iped.engine.webapi;

import java.util.List;
import java.util.UUID;

/**
 * Abstraction over the engine's processing orchestrator, exposed to the webapi layer without
 * creating a compile-time dependency on engine internals. Implementations live in iped-engine and
 * are registered via {@link ProcessingServiceRegistry}.
 */
public interface IProcessingService {

  List<UUID> getActiveCaseIds();

  int getActiveCaseCount();

  /** Returns the case state string, or {@code null} if the case is not found. */
  String getCaseState(UUID caseId);

  boolean caseExists(UUID caseId);

  void pauseCase(UUID caseId);

  void resumeCase(UUID caseId);

  int getMaxConcurrentCases();

  long getMaxMemoryPerCase();

  long getTotalMemoryUsage();

  long getMemoryUsage(UUID caseId);
}
