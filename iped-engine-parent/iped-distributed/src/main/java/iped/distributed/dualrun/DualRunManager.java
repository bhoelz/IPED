package iped.distributed.dualrun;

import iped.distributed.status.ItemStatusEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import lombok.extern.slf4j.Slf4j;

/**
 * Coordinator-side manager for dual-run sessions.
 *
 * <p>One {@link DualRunSession} is maintained per case. Sessions are started explicitly via {@link
 * #startSession(String)} — typically when a case is registered and dual-run mode is enabled. Event
 * routing ({@link #recordEvent}) is a no-op for cases without an active session, so this manager
 * can be wired into the event consumer unconditionally.
 *
 * <p>The {@link #submitReference} and {@link #getReport} methods back the two dual-run REST
 * endpoints on the coordinator.
 */
@Slf4j
public class DualRunManager {

  private final ConcurrentHashMap<String, DualRunSession> sessions = new ConcurrentHashMap<>();

  /**
   * Create (or return the existing) dual-run session for the given case. Safe to call multiple
   * times for the same case.
   */
  public DualRunSession startSession(String caseId) {
    return sessions.computeIfAbsent(
        caseId,
        id -> {
          log.info("Dual-run session opened for case '{}'", id);
          return new DualRunSession(id);
        });
  }

  /** Route a status event to the matching session (no-op if no session for the case). */
  public void recordEvent(ItemStatusEvent event) {
    DualRunSession session = sessions.get(event.getCaseId());
    if (session != null) session.recordEvent(event);
  }

  /**
   * Submit the reference (monolithic) item list for a case.
   *
   * @return false if no session exists for this case (call {@link #startSession} first)
   */
  public boolean submitReference(String caseId, List<ItemSummary> items) {
    DualRunSession session = sessions.get(caseId);
    if (session == null) return false;
    session.setReference(items);
    return true;
  }

  /**
   * Generate and return the current comparison report for a case.
   *
   * @return null if no session exists for this case
   */
  public DualRunReport getReport(String caseId) {
    DualRunSession session = sessions.get(caseId);
    return session != null ? session.generateReport() : null;
  }

  public boolean hasSession(String caseId) {
    return sessions.containsKey(caseId);
  }

  public List<String> activeSessionCaseIds() {
    return new ArrayList<>(sessions.keySet());
  }
}
