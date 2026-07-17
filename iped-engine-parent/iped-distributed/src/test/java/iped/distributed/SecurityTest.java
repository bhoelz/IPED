package iped.distributed;

import static org.junit.jupiter.api.Assertions.*;

import iped.distributed.audit.ProcessingAuditLog;
import iped.distributed.audit.ProcessingRecord;
import iped.distributed.config.DistributedConfig;
import iped.distributed.kafka.KafkaItemMessage;
import iped.distributed.security.KafkaSecurityConfigurer;
import iped.distributed.security.PayloadSigner;
import iped.distributed.status.ItemStatusEvent;
import iped.utils.UTF8Properties;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.Properties;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class SecurityTest {

  // ── KafkaSecurityConfigurer — no-op when disabled ────────────────────────

  @Test
  void noPropertiesWhenDisabled() {
    Properties p = new Properties();
    KafkaSecurityConfigurer.apply(p, cfg());
    assertTrue(p.isEmpty(), "no security properties should be added when disabled");
  }

  // ── KafkaSecurityConfigurer — TLS only ───────────────────────────────────

  @Test
  void tlsOnlySetsProtocolSsl() {
    Properties p = new Properties();
    KafkaSecurityConfigurer.apply(p, cfg("kafkaTlsEnabled", "true"));
    assertEquals("SSL", p.getProperty("security.protocol"));
    assertNull(p.getProperty("sasl.mechanism"));
  }

  @Test
  void tlsWithTruststoreAddsProperties() {
    Properties p = new Properties();
    KafkaSecurityConfigurer.apply(
        p,
        cfg(
            "kafkaTlsEnabled", "true",
            "kafkaTruststorePath", "/etc/certs/truststore.jks",
            "kafkaTruststorePassword", "s3cr3t"));
    assertEquals("/etc/certs/truststore.jks", p.getProperty("ssl.truststore.location"));
    assertEquals("s3cr3t", p.getProperty("ssl.truststore.password"));
  }

  @Test
  void tlsWithKeystoreAddsAllKeyProperties() {
    Properties p = new Properties();
    KafkaSecurityConfigurer.apply(
        p,
        cfg(
            "kafkaTlsEnabled", "true",
            "kafkaTruststorePath", "/ts.jks",
            "kafkaTruststorePassword", "tp",
            "kafkaKeystorePath", "/ks.jks",
            "kafkaKeystorePassword", "kp",
            "kafkaKeyPassword", "keyp"));
    assertEquals("/ks.jks", p.getProperty("ssl.keystore.location"));
    assertEquals("kp", p.getProperty("ssl.keystore.password"));
    assertEquals("keyp", p.getProperty("ssl.key.password"));
  }

  // ── KafkaSecurityConfigurer — SASL ───────────────────────────────────────

  @Test
  void saslPlainWithoutTlsUsesSaslPlaintext() {
    Properties p = new Properties();
    KafkaSecurityConfigurer.apply(
        p,
        cfg(
            "kafkaSaslMechanism", "PLAIN",
            "kafkaSaslUsername", "user",
            "kafkaSaslPassword", "pass"));
    assertEquals("SASL_PLAINTEXT", p.getProperty("security.protocol"));
    assertEquals("PLAIN", p.getProperty("sasl.mechanism"));
    String jaas = p.getProperty("sasl.jaas.config");
    assertNotNull(jaas);
    assertTrue(jaas.contains("PlainLoginModule"));
    assertTrue(jaas.contains("username=\"user\""));
    assertTrue(jaas.contains("password=\"pass\""));
  }

  @Test
  void saslScramWithTlsUsesSaslSsl() {
    Properties p = new Properties();
    KafkaSecurityConfigurer.apply(
        p,
        cfg(
            "kafkaTlsEnabled", "true",
            "kafkaSaslMechanism", "SCRAM-SHA-256",
            "kafkaSaslUsername", "admin",
            "kafkaSaslPassword", "pw"));
    assertEquals("SASL_SSL", p.getProperty("security.protocol"));
    assertEquals("SCRAM-SHA-256", p.getProperty("sasl.mechanism"));
    String jaas = p.getProperty("sasl.jaas.config");
    assertTrue(jaas.contains("ScramLoginModule"));
  }

  @Test
  void saslScramSha512Supported() {
    Properties p = new Properties();
    KafkaSecurityConfigurer.apply(
        p,
        cfg(
            "kafkaSaslMechanism", "SCRAM-SHA-512",
            "kafkaSaslUsername", "u",
            "kafkaSaslPassword", "p"));
    assertEquals("SCRAM-SHA-512", p.getProperty("sasl.mechanism"));
  }

  @Test
  void unsupportedMechanismThrows() {
    assertThrows(
        IllegalArgumentException.class,
        () -> KafkaSecurityConfigurer.buildJaas("GSSAPI", "u", "p"));
  }

  // ── PayloadSigner — basic sign / verify ──────────────────────────────────

  @Test
  void isEnabledOnlyWhenSecretNonBlank() {
    assertFalse(PayloadSigner.isEnabled(null));
    assertFalse(PayloadSigner.isEnabled(""));
    assertFalse(PayloadSigner.isEnabled("   "));
    assertTrue(PayloadSigner.isEnabled("s3cr3t"));
  }

  @Test
  void signProducesHexString() {
    KafkaItemMessage msg = message("case-1", "item-1", 0, "/path", 100L);
    PayloadSigner.sign(msg, "secret");
    String sig = msg.getSignature();
    assertNotNull(sig);
    assertFalse(sig.isBlank());
    assertTrue(sig.matches("[0-9a-f]+"), "signature should be lowercase hex");
  }

  @Test
  void verifyReturnsTrueForCorrectSignature() {
    KafkaItemMessage msg = message("case-1", "item-1", 1, "/a/b/c", 500L);
    PayloadSigner.sign(msg, "shared-secret");
    assertTrue(PayloadSigner.verify(msg, "shared-secret"));
  }

  @Test
  void verifyReturnsFalseForWrongSecret() {
    KafkaItemMessage msg = message("case-1", "item-1", 1, "/a", 100L);
    PayloadSigner.sign(msg, "correct-secret");
    assertFalse(PayloadSigner.verify(msg, "wrong-secret"));
  }

  @Test
  void verifyReturnsFalseForNullSignature() {
    KafkaItemMessage msg = message("case-1", "item-1", 0, "/p", 0L);
    // no sign() call
    assertFalse(PayloadSigner.verify(msg, "any-secret"));
  }

  @Test
  void verifyReturnsFalseForTamperedItemUuid() {
    KafkaItemMessage msg = message("case-1", "item-1", 0, "/p", 100L);
    PayloadSigner.sign(msg, "secret");
    msg.setItemUuid("item-tampered");
    assertFalse(PayloadSigner.verify(msg, "secret"));
  }

  @Test
  void verifyReturnsFalseForTamperedPath() {
    KafkaItemMessage msg = message("case-1", "item-1", 0, "/original/path", 100L);
    PayloadSigner.sign(msg, "secret");
    msg.setPath("/tampered/path");
    assertFalse(PayloadSigner.verify(msg, "secret"));
  }

  @Test
  void verifyReturnsFalseForTamperedLength() {
    KafkaItemMessage msg = message("case-1", "item-1", 0, "/p", 100L);
    PayloadSigner.sign(msg, "secret");
    msg.setLength(999L);
    assertFalse(PayloadSigner.verify(msg, "secret"));
  }

  @Test
  void signRoundtripWithNullFields() {
    KafkaItemMessage msg = message("case-2", "item-2", 0, null, null);
    PayloadSigner.sign(msg, "s");
    assertTrue(PayloadSigner.verify(msg, "s"));
  }

  @Test
  void signingContentIsDeterministic() {
    KafkaItemMessage a = message("c", "u", 3, "/x", 42L);
    KafkaItemMessage b = message("c", "u", 3, "/x", 42L);
    assertEquals(PayloadSigner.signingContent(a), PayloadSigner.signingContent(b));
  }

  // ── ProcessingRecord — factory from event ────────────────────────────────

  @Test
  void recordFromCompletedEvent() {
    ItemStatusEvent e =
        event(
            "case-A",
            "item-1",
            "HashTask",
            1,
            ItemStatusEvent.Type.COMPLETED,
            250L,
            null,
            "agent-42");
    ProcessingRecord r = ProcessingRecord.from(e);
    assertNotNull(r);
    assertEquals("case-A", r.caseId());
    assertEquals("item-1", r.itemUuid());
    assertEquals("HashTask", r.taskType());
    assertEquals(1, r.pipelineStage());
    assertEquals("agent-42", r.agentId());
    assertEquals("COMPLETED", r.outcome());
    assertNull(r.errorMessage());
    assertEquals(250L, r.durationMs());
  }

  @Test
  void recordFromErrorEvent() {
    ItemStatusEvent e =
        event("c", "i", "OcrTask", 2, ItemStatusEvent.Type.ERROR, 100L, "out of memory", null);
    ProcessingRecord r = ProcessingRecord.from(e);
    assertNotNull(r);
    assertEquals("ERROR", r.outcome());
    assertEquals("out of memory", r.errorMessage());
  }

  @Test
  void recordFromTimeoutEvent() {
    ItemStatusEvent e =
        event("c", "i", "HashTask", 1, ItemStatusEvent.Type.TIMEOUT, 3600000L, "timed out", null);
    ProcessingRecord r = ProcessingRecord.from(e);
    assertEquals("TIMEOUT", r.outcome());
  }

  @Test
  void recordFromDiscoveredIsNull() {
    ItemStatusEvent e = new ItemStatusEvent();
    e.setType(ItemStatusEvent.Type.DISCOVERED);
    assertNull(ProcessingRecord.from(e));
  }

  @Test
  void recordFromStartedIsNull() {
    ItemStatusEvent e = new ItemStatusEvent();
    e.setType(ItemStatusEvent.Type.STARTED);
    assertNull(ProcessingRecord.from(e));
  }

  // ── ProcessingAuditLog — accumulation and query ──────────────────────────

  @Test
  void auditLogAccumulatesCompletedEvents() {
    ProcessingAuditLog log = new ProcessingAuditLog();
    log.record(event("c1", "i1", "HashTask", 1, ItemStatusEvent.Type.COMPLETED, 100L, null, "a1"));
    log.record(event("c1", "i2", "HashTask", 1, ItemStatusEvent.Type.COMPLETED, 200L, null, "a2"));
    assertEquals(2, log.totalRecords("c1"));
  }

  @Test
  void auditLogIgnoresNonTerminalEvents() {
    ProcessingAuditLog log = new ProcessingAuditLog();
    log.record(event("c1", "i1", "T", 1, ItemStatusEvent.Type.DISCOVERED, 0, null, null));
    log.record(event("c1", "i1", "T", 1, ItemStatusEvent.Type.STARTED, 0, null, null));
    assertEquals(0, log.totalRecords("c1"));
  }

  @Test
  void auditLogForCaseReturnsEmptyForUnknown() {
    ProcessingAuditLog log = new ProcessingAuditLog();
    assertTrue(log.forCase("unknown").isEmpty());
  }

  @Test
  void auditLogForItemFiltersCorrectly() {
    ProcessingAuditLog log = new ProcessingAuditLog();
    log.record(event("c1", "i1", "T1", 1, ItemStatusEvent.Type.COMPLETED, 50L, null, "a"));
    log.record(event("c1", "i2", "T1", 1, ItemStatusEvent.Type.COMPLETED, 60L, null, "a"));
    log.record(event("c1", "i1", "T2", 2, ItemStatusEvent.Type.COMPLETED, 70L, null, "a"));

    List<ProcessingRecord> forI1 = log.forItem("c1", "i1");
    assertEquals(2, forI1.size());
    assertTrue(forI1.stream().allMatch(r -> "i1".equals(r.itemUuid())));
  }

  @Test
  void auditLogMultipleCasesIsolated() {
    ProcessingAuditLog log = new ProcessingAuditLog();
    log.record(event("case-A", "x", "T", 1, ItemStatusEvent.Type.COMPLETED, 10L, null, null));
    log.record(event("case-B", "y", "T", 1, ItemStatusEvent.Type.ERROR, 20L, "err", null));

    assertEquals(1, log.totalRecords("case-A"));
    assertEquals(1, log.totalRecords("case-B"));
    assertEquals("COMPLETED", log.forCase("case-A").get(0).outcome());
    assertEquals("ERROR", log.forCase("case-B").get(0).outcome());
  }

  @Test
  void auditLogCsvExportContainsHeader() {
    ProcessingAuditLog log = new ProcessingAuditLog();
    String csv = log.exportCsv("unknown");
    assertTrue(csv.startsWith(ProcessingAuditLog.CSV_HEADER));
  }

  @Test
  void auditLogCsvExportContainsRecords() {
    ProcessingAuditLog log = new ProcessingAuditLog();
    log.record(
        event(
            "c1",
            "item-uuid",
            "HashTask",
            1,
            ItemStatusEvent.Type.COMPLETED,
            300L,
            null,
            "agent-1"));
    String csv = log.exportCsv("c1");
    assertTrue(csv.contains("item-uuid"));
    assertTrue(csv.contains("HashTask"));
    assertTrue(csv.contains("COMPLETED"));
    assertTrue(csv.contains("agent-1"));
  }

  @Test
  void auditLogCsvEscapesCommasInFields() {
    ProcessingAuditLog log = new ProcessingAuditLog();
    log.record(
        event(
            "c1",
            "i1",
            "T",
            1,
            ItemStatusEvent.Type.ERROR,
            0L,
            "error: something, went wrong",
            null));
    String csv = log.exportCsv("c1");
    // The comma-containing error message should be quoted
    assertTrue(csv.contains("\"error: something, went wrong\""));
  }

  // ── ProcessingAuditLog — file persistence ────────────────────────────────

  @Test
  void auditLogPersistsToFile(@TempDir Path tmpDir) throws Exception {
    Path logFile = tmpDir.resolve("audit.csv");
    ProcessingAuditLog log = new ProcessingAuditLog(logFile);
    log.record(
        event(
            "case-X", "item-Y", "Task1", 1, ItemStatusEvent.Type.COMPLETED, 100L, null, "agentZ"));

    assertTrue(Files.exists(logFile));
    String content = Files.readString(logFile);
    assertTrue(content.contains("item-Y"));
    assertTrue(content.contains("COMPLETED"));
    assertTrue(content.contains("agentZ"));
  }

  @Test
  void auditLogFileAppendsAcrossInstances(@TempDir Path tmpDir) throws Exception {
    Path logFile = tmpDir.resolve("audit.csv");
    // First instance writes one record
    new ProcessingAuditLog(logFile)
        .record(event("c", "i1", "T", 1, ItemStatusEvent.Type.COMPLETED, 10L, null, null));
    // Second instance opens in append mode
    new ProcessingAuditLog(logFile)
        .record(event("c", "i2", "T", 1, ItemStatusEvent.Type.COMPLETED, 20L, null, null));

    String content = Files.readString(logFile);
    assertTrue(content.contains("i1"));
    assertTrue(content.contains("i2"));
    // Should only have one header line
    long headerCount = content.lines().filter(l -> l.startsWith("caseId")).count();
    assertEquals(1, headerCount);
  }

  // ── Helpers ──────────────────────────────────────────────────────────────

  private static KafkaItemMessage message(
      String caseId, String uuid, int stage, String path, Long length) {
    KafkaItemMessage m = new KafkaItemMessage();
    m.setCaseId(caseId);
    m.setItemUuid(uuid);
    m.setPipelineStage(stage);
    m.setPath(path);
    m.setLength(length);
    return m;
  }

  private static ItemStatusEvent event(
      String caseId,
      String itemUuid,
      String taskType,
      int stage,
      ItemStatusEvent.Type type,
      long durationMs,
      String errorMsg,
      String agentId) {
    ItemStatusEvent e = new ItemStatusEvent();
    e.setCaseId(caseId);
    e.setItemUuid(itemUuid);
    e.setTaskType(taskType);
    e.setPipelineStage(stage);
    e.setType(type);
    e.setDurationMs(durationMs);
    e.setErrorMessage(errorMsg);
    e.setAgentId(agentId);
    e.setTimestamp(Instant.now());
    return e;
  }

  /** Build a DistributedConfig with zero or more key=value pairs applied. */
  private static DistributedConfig cfg(String... kvPairs) {
    DistributedConfig c = new DistributedConfig();
    if (kvPairs.length > 0) {
      UTF8Properties p = new UTF8Properties();
      for (int i = 0; i < kvPairs.length - 1; i += 2) {
        p.setProperty(kvPairs[i], kvPairs[i + 1]);
      }
      c.processProperties(p);
    }
    return c;
  }
}
