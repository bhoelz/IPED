package iped.engine.additionalindex;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class InMemoryVectorStoreConnectorTest {
  @Test
  void indexesVectorsAndReturnsTraceableSimilarityMatches() throws Exception {
    try (var connector = new InMemoryVectorStoreConnector(2)) {
      connector.open();
      connector.indexVector(10, "test-model", new float[] {1, 0});
      connector.indexVector(20, "test-model", new float[] {0, 1});
      var matches = connector.similaritySearch(new float[] {1, 0}, 2);
      assertEquals(10, matches.getFirst().itemId());
      assertTrue(matches.getFirst().score() > matches.get(1).score());
      assertTrue(connector.supportsItemIdTraceability());
    }
  }
}
