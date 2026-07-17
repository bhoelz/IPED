package iped.distributed;

import static org.junit.jupiter.api.Assertions.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import iped.distributed.kafka.KafkaItemMessage;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class KafkaItemMessageTest {

  // ---- Identity fields ----

  @Test
  void caseId_roundTrip() {
    KafkaItemMessage msg = new KafkaItemMessage();
    msg.setCaseId("case-abc");
    assertEquals("case-abc", msg.getCaseId());
  }

  @Test
  void itemUuid_roundTrip() {
    KafkaItemMessage msg = new KafkaItemMessage();
    msg.setItemUuid("550e8400-e29b-41d4-a716-446655440000");
    assertEquals("550e8400-e29b-41d4-a716-446655440000", msg.getItemUuid());
  }

  @Test
  void localItemId_roundTrip() {
    KafkaItemMessage msg = new KafkaItemMessage();
    msg.setLocalItemId(42);
    assertEquals(42, msg.getLocalItemId());
  }

  @Test
  void dataSourceUuid_roundTrip() {
    KafkaItemMessage msg = new KafkaItemMessage();
    msg.setDataSourceUuid("ds-uuid-1");
    assertEquals("ds-uuid-1", msg.getDataSourceUuid());
  }

  @Test
  void dataSourceName_roundTrip() {
    KafkaItemMessage msg = new KafkaItemMessage();
    msg.setDataSourceName("evidence.e01");
    assertEquals("evidence.e01", msg.getDataSourceName());
  }

  // ---- Pipeline position ----

  @Test
  void pipelineStage_roundTrip() {
    KafkaItemMessage msg = new KafkaItemMessage();
    msg.setPipelineStage(3);
    assertEquals(3, msg.getPipelineStage());
  }

  @Test
  void priority_roundTrip() {
    KafkaItemMessage msg = new KafkaItemMessage();
    msg.setPriority(true);
    assertTrue(msg.isPriority());
    msg.setPriority(false);
    assertFalse(msg.isPriority());
  }

  // ---- Item properties ----

  @Test
  void path_roundTrip() {
    KafkaItemMessage msg = new KafkaItemMessage();
    msg.setPath("/evidence/root/file.txt");
    assertEquals("/evidence/root/file.txt", msg.getPath());
  }

  @Test
  void name_roundTrip() {
    KafkaItemMessage msg = new KafkaItemMessage();
    msg.setName("document.pdf");
    assertEquals("document.pdf", msg.getName());
  }

  @Test
  void extension_roundTrip() {
    KafkaItemMessage msg = new KafkaItemMessage();
    msg.setExtension("pdf");
    assertEquals("pdf", msg.getExtension());
  }

  @Test
  void length_roundTrip() {
    KafkaItemMessage msg = new KafkaItemMessage();
    msg.setLength(1024L);
    assertEquals(1024L, msg.getLength());
  }

  @Test
  void fileOffset_roundTrip() {
    KafkaItemMessage msg = new KafkaItemMessage();
    msg.setFileOffset(512L);
    assertEquals(512L, msg.getFileOffset());
  }

  @Test
  void booleanFlags_roundTrip() {
    KafkaItemMessage msg = new KafkaItemMessage();
    msg.setDir(true);
    msg.setDeleted(true);
    msg.setCarved(true);
    msg.setSubItem(true);
    msg.setRoot(true);
    msg.setHasChildren(true);
    msg.setSumVolume(true);

    assertTrue(msg.isDir());
    assertTrue(msg.isDeleted());
    assertTrue(msg.isCarved());
    assertTrue(msg.isSubItem());
    assertTrue(msg.isRoot());
    assertTrue(msg.isHasChildren());
    assertTrue(msg.isSumVolume());
  }

  @Test
  void subitemId_roundTrip() {
    KafkaItemMessage msg = new KafkaItemMessage();
    msg.setSubitemId(7);
    assertEquals(7, msg.getSubitemId());
  }

  @Test
  void parentItemUuid_roundTrip() {
    KafkaItemMessage msg = new KafkaItemMessage();
    msg.setParentItemUuid("parent-uuid");
    assertEquals("parent-uuid", msg.getParentItemUuid());
  }

  @Test
  void parentItemUuids_roundTrip() {
    KafkaItemMessage msg = new KafkaItemMessage();
    List<String> uuids = Arrays.asList("u1", "u2");
    msg.setParentItemUuids(uuids);
    assertEquals(uuids, msg.getParentItemUuids());
  }

  // ---- Dates ----

  @Test
  void dates_roundTrip() {
    KafkaItemMessage msg = new KafkaItemMessage();
    Date d = new Date(1_000_000L);
    msg.setAccessDate(d);
    msg.setCreationDate(d);
    msg.setModificationDate(d);
    msg.setChangeDate(d);

    assertEquals(d, msg.getAccessDate());
    assertEquals(d, msg.getCreationDate());
    assertEquals(d, msg.getModificationDate());
    assertEquals(d, msg.getChangeDate());
  }

  // ---- Analysis state ----

  @Test
  void hash_roundTrip() {
    KafkaItemMessage msg = new KafkaItemMessage();
    msg.setHash("aabbccddeeff");
    assertEquals("aabbccddeeff", msg.getHash());
  }

  @Test
  void mediaType_roundTrip() {
    KafkaItemMessage msg = new KafkaItemMessage();
    msg.setMediaType("application/pdf");
    assertEquals("application/pdf", msg.getMediaType());
  }

  @Test
  void metadata_roundTrip() {
    KafkaItemMessage msg = new KafkaItemMessage();
    Map<String, List<String>> meta = Map.of("Author", Arrays.asList("Alice"));
    msg.setMetadata(meta);
    assertEquals(meta, msg.getMetadata());
  }

  @Test
  void extraAttributes_roundTrip() {
    KafkaItemMessage msg = new KafkaItemMessage();
    Map<String, Object> attrs = Map.of("score", 0.95);
    msg.setExtraAttributes(attrs);
    assertEquals(attrs, msg.getExtraAttributes());
  }

  // ---- Content reference ----

  @Test
  void inputStreamFactoryClass_roundTrip() {
    KafkaItemMessage msg = new KafkaItemMessage();
    msg.setInputStreamFactoryClass("com.example.Factory");
    assertEquals("com.example.Factory", msg.getInputStreamFactoryClass());
  }

  @Test
  void inputStreamFactoryParams_roundTrip() {
    KafkaItemMessage msg = new KafkaItemMessage();
    Map<String, String> params = Map.of("path", "/data/evidence.e01");
    msg.setInputStreamFactoryParams(params);
    assertEquals(params, msg.getInputStreamFactoryParams());
  }

  @Test
  void idInDataSource_roundTrip() {
    KafkaItemMessage msg = new KafkaItemMessage();
    msg.setIdInDataSource("obj-1234");
    assertEquals("obj-1234", msg.getIdInDataSource());
  }

  // ---- Case paths ----

  @Test
  void caseOutputPath_roundTrip() {
    KafkaItemMessage msg = new KafkaItemMessage();
    msg.setCaseOutputPath("/mnt/nas/case-output");
    assertEquals("/mnt/nas/case-output", msg.getCaseOutputPath());
  }

  @Test
  void sharedStorageRoot_roundTrip() {
    KafkaItemMessage msg = new KafkaItemMessage();
    msg.setSharedStorageRoot("/mnt/nas");
    assertEquals("/mnt/nas", msg.getSharedStorageRoot());
  }

  // ---- toString ----

  @Test
  void toString_containsKeyFields() {
    KafkaItemMessage msg = new KafkaItemMessage();
    msg.setCaseId("myCase");
    msg.setItemUuid("item-uuid-1");
    msg.setPipelineStage(2);
    msg.setPath("/root/file.txt");

    String str = msg.toString();
    assertNotNull(str);
    assertTrue(str.contains("myCase"), "toString should contain caseId");
    assertTrue(str.contains("item-uuid-1"), "toString should contain itemUuid");
  }

  // ---- Jackson serialization ----

  @Test
  void jacksonRoundTrip_preservesBasicFields() throws Exception {
    ObjectMapper mapper = new ObjectMapper();

    KafkaItemMessage original = new KafkaItemMessage();
    original.setCaseId("case-42");
    original.setItemUuid("uuid-1");
    original.setLocalItemId(7);
    original.setPipelineStage(1);
    original.setPath("/evidence/file.txt");
    original.setName("file.txt");
    original.setHash("deadbeef");

    String json = mapper.writeValueAsString(original);
    KafkaItemMessage restored = mapper.readValue(json, KafkaItemMessage.class);

    assertEquals("case-42", restored.getCaseId());
    assertEquals("uuid-1", restored.getItemUuid());
    assertEquals(7, restored.getLocalItemId());
    assertEquals(1, restored.getPipelineStage());
    assertEquals("/evidence/file.txt", restored.getPath());
    assertEquals("file.txt", restored.getName());
    assertEquals("deadbeef", restored.getHash());
  }

  @Test
  void defaultConstructor_booleans_defaultFalse() {
    KafkaItemMessage msg = new KafkaItemMessage();
    assertFalse(msg.isDir());
    assertFalse(msg.isDeleted());
    assertFalse(msg.isCarved());
    assertFalse(msg.isSubItem());
    assertFalse(msg.isRoot());
    assertFalse(msg.isHasChildren());
    assertFalse(msg.isSumVolume());
    assertFalse(msg.isPriority());
  }
}
