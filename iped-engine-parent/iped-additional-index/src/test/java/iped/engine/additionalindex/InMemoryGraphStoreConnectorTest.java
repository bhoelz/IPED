package iped.engine.additionalindex;

import static org.junit.jupiter.api.Assertions.*;

import iped.datasource.EvidenceRelationship;
import java.util.Map;
import org.junit.jupiter.api.Test;

class InMemoryGraphStoreConnectorTest {
  @Test
  void sharesCanonicalRelationshipModelAndQueriesByEvidenceItem() throws Exception {
    try (var connector = new InMemoryGraphStoreConnector()) {
      connector.upsertRelationship(
          new EvidenceRelationship(1, 2, "CONTACT", Map.of("source", "email")));
      var found = connector.findRelationships(2, 10);
      assertEquals(1, found.size());
      assertEquals(1, found.getFirst().sourceItemId());
      assertTrue(connector.supportsItemIdTraceability());
    }
  }
}
