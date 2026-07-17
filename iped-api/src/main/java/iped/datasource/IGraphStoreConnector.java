package iped.datasource;

import java.io.IOException;
import java.util.List;

/** Pluggable graph backend contract using the canonical evidence relationship model. */
public interface IGraphStoreConnector extends IAdditionalStoreConnector {
    void upsertRelationship(EvidenceRelationship relationship) throws IOException;
    List<EvidenceRelationship> findRelationships(int itemId, int limit) throws IOException;
}
