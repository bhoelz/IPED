package iped.datasource;

import java.io.IOException;
import java.util.List;

/** Additional-store contract for embedding vectors and similarity queries. */
public interface IVectorStoreConnector extends IAdditionalStoreConnector {
    void indexVector(int itemId, String model, float[] vector) throws IOException;
    List<VectorMatch> similaritySearch(float[] query, int limit) throws IOException;
    int dimension();
}
