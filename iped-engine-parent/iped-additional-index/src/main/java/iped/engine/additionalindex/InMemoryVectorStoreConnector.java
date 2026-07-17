package iped.engine.additionalindex;

import iped.datasource.AdditionalStoreConsistencyPolicy;
import iped.datasource.IVectorStoreConnector;
import iped.datasource.VectorMatch;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/** Reference connector for tests and local pilots; production stores implement the same SPI. */
public final class InMemoryVectorStoreConnector implements IVectorStoreConnector {
  private final int dimension;
  private final Map<Integer, float[]> vectors = new ConcurrentHashMap<>();

  public InMemoryVectorStoreConnector(int dimension) {
    if (dimension <= 0) throw new IllegalArgumentException("dimension must be positive");
    this.dimension = dimension;
  }

  @Override
  public void open() {}

  @Override
  public void index(int itemId, String taskName, Map<String, Object> attributes) {}

  @Override
  public void indexVector(int itemId, String model, float[] vector) {
    validate(vector);
    vectors.put(itemId, vector.clone());
  }

  @Override
  public List<VectorMatch> similaritySearch(float[] query, int limit) {
    validate(query);
    return vectors.entrySet().stream()
        .map(e -> new VectorMatch(e.getKey(), cosine(query, e.getValue())))
        .sorted(Comparator.comparing(VectorMatch::score).reversed())
        .limit(Math.max(0, limit))
        .toList();
  }

  @Override
  public int dimension() {
    return dimension;
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
    vectors.clear();
  }

  private void validate(float[] vector) {
    if (vector == null || vector.length != dimension)
      throw new IllegalArgumentException("vector dimension mismatch");
  }

  private static float cosine(float[] a, float[] b) {
    double dot = 0, aa = 0, bb = 0;
    for (int i = 0; i < a.length; i++) {
      dot += a[i] * b[i];
      aa += a[i] * a[i];
      bb += b[i] * b[i];
    }
    return aa == 0 || bb == 0 ? 0f : (float) (dot / (Math.sqrt(aa) * Math.sqrt(bb)));
  }
}
