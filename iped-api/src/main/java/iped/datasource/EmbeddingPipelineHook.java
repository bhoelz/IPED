package iped.datasource;

/** Pluggable embedding-generation hook used by a processing task. */
@FunctionalInterface
public interface EmbeddingPipelineHook {
  float[] embed(int itemId, String text) throws Exception;
}
