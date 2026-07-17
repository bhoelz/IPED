package iped.engine.additionalindex;

import iped.datasource.*;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

/** Reference graph connector proving backend selection can be independent of Neo4j. */
public final class InMemoryGraphStoreConnector implements IGraphStoreConnector {
  private final List<EvidenceRelationship> relationships = new CopyOnWriteArrayList<>();

  @Override
  public void open() {}

  @Override
  public void index(int itemId, String taskName, Map<String, Object> attributes) {}

  @Override
  public void upsertRelationship(EvidenceRelationship relationship) {
    relationships.removeIf(
        r ->
            r.sourceItemId() == relationship.sourceItemId()
                && r.targetItemId() == relationship.targetItemId()
                && r.type().equals(relationship.type()));
    relationships.add(Objects.requireNonNull(relationship));
  }

  @Override
  public List<EvidenceRelationship> findRelationships(int itemId, int limit) {
    return relationships.stream()
        .filter(r -> r.sourceItemId() == itemId || r.targetItemId() == itemId)
        .limit(Math.max(0, limit))
        .toList();
  }

  @Override
  public void commit() {}

  @Override
  public boolean supportsItemIdTraceability() {
    return true;
  }

  @Override
  public AdditionalStoreConsistencyPolicy consistencyPolicy() {
    return AdditionalStoreConsistencyPolicy.COMMIT_ATOMIC;
  }

  @Override
  public void close() {
    relationships.clear();
  }
}
