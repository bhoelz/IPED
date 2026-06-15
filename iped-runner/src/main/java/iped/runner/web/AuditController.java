package iped.runner.web;

import iped.runner.distributed.ProcessingAuditLog;
import iped.runner.distributed.ProcessingRecord;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * REST endpoints for the processing chain-of-custody audit log.
 *
 * <p>All endpoints return 404 when no records exist for the requested case,
 * so the UI can distinguish "case not found" from "case has no terminal events yet."
 */
@RestController
@RequestMapping("/api/v1/audit")
@RequiredArgsConstructor
public class AuditController {

    private final ProcessingAuditLog auditLog;

    /** All terminal records for a case, newest first. */
    @GetMapping("/{caseId}")
    public ResponseEntity<Map<String, Object>> getCaseAudit(@PathVariable String caseId) {
        List<ProcessingRecord> records = auditLog.forCase(caseId);
        if (records.isEmpty()) return ResponseEntity.notFound().build();
        return ResponseEntity.ok(Map.of(
                "caseId",  caseId,
                "total",   records.size(),
                "records", records.reversed()
        ));
    }

    /** All terminal records for a case as CSV (RFC 4180). */
    @GetMapping(value = "/{caseId}/csv", produces = "text/csv")
    public ResponseEntity<String> getCaseAuditCsv(@PathVariable String caseId) {
        List<ProcessingRecord> records = auditLog.forCase(caseId);
        if (records.isEmpty()) return ResponseEntity.notFound().build();
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("text/csv"))
                .header("Content-Disposition",
                        "attachment; filename=\"audit-" + caseId + ".csv\"")
                .body(auditLog.exportCsv(caseId));
    }

    /** The most recent terminal record for a specific item. */
    @GetMapping("/{caseId}/{itemUuid}")
    public ResponseEntity<ProcessingRecord> getItemAudit(@PathVariable String caseId,
                                                          @PathVariable String itemUuid) {
        Optional<ProcessingRecord> latest = auditLog.latestForItem(caseId, itemUuid);
        return latest.map(ResponseEntity::ok)
                     .orElseGet(() -> ResponseEntity.notFound().build());
    }
}
