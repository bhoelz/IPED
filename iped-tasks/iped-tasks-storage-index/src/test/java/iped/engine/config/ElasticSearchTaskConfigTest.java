package iped.engine.config;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class ElasticSearchTaskConfigTest {

  @Test
  void getTaskEnableProperty_isNonBlank() {
    assertFalse(new ElasticSearchTaskConfig().getTaskEnableProperty().isBlank());
  }

  @Test
  void getTaskConfigFileName_isNonBlank() {
    assertFalse(new ElasticSearchTaskConfig().getTaskConfigFileName().isBlank());
  }

  @Test
  void defaults_port() {
    assertEquals(9200, new ElasticSearchTaskConfig().getPort());
  }

  @Test
  void defaults_maxFields() {
    assertEquals(10000, new ElasticSearchTaskConfig().getMax_fields());
  }

  @Test
  void defaults_minBulkSize() {
    assertEquals(1 << 23, new ElasticSearchTaskConfig().getMin_bulk_size());
  }

  @Test
  void defaults_minBulkItems() {
    assertEquals(1000, new ElasticSearchTaskConfig().getMin_bulk_items());
  }

  @Test
  void defaults_connectTimeout() {
    assertEquals(5000, new ElasticSearchTaskConfig().getConnect_timeout());
  }

  @Test
  void defaults_timeoutMillis() {
    assertEquals(3600000, new ElasticSearchTaskConfig().getTimeout_millis());
  }

  @Test
  void defaults_maxAsyncRequests() {
    assertEquals(5, new ElasticSearchTaskConfig().getMax_async_requests());
  }

  @Test
  void defaults_indexShards() {
    assertEquals(1, new ElasticSearchTaskConfig().getIndex_shards());
  }

  @Test
  void defaults_indexReplicas() {
    assertEquals(1, new ElasticSearchTaskConfig().getIndex_replicas());
  }

  @Test
  void defaults_indexPolicyEmpty() {
    assertEquals("", new ElasticSearchTaskConfig().getIndex_policy());
  }

  @Test
  void defaults_hostNull() {
    assertNull(new ElasticSearchTaskConfig().getHost());
  }

  @Test
  void defaults_validateSSLTrue() {
    assertTrue(new ElasticSearchTaskConfig().getValidateSSL());
  }

  @Test
  void defaults_termVectorFalse() {
    assertFalse(new ElasticSearchTaskConfig().isTermVector());
  }

  @Test
  void defaults_useCustomAnalyzerFalse() {
    assertFalse(new ElasticSearchTaskConfig().isUseCustomAnalyzer());
  }

  @Test
  void getConfiguration_whenNew_thenNotNull() {
    assertNotNull(new ElasticSearchTaskConfig().getConfiguration());
  }
}
