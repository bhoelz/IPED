package iped.engine.webapi.json;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;

class DataListJSONTest {

  @Test
  void constructor_withArray_thenGetDataReturnsList() {
    String[] items = {"a", "b", "c"};
    DataListJSON<String> list = new DataListJSON<>(items);
    assertEquals(Arrays.asList(items), list.getData());
  }

  @Test
  void constructor_withList_thenGetDataReturnsSameList() {
    List<Integer> items = List.of(1, 2, 3);
    DataListJSON<Integer> list = new DataListJSON<>(items);
    assertEquals(items, list.getData());
  }

  @Test
  void getData_whenEmpty_thenEmptyList() {
    DataListJSON<String> list = new DataListJSON<>(new String[] {});
    assertNotNull(list.getData());
    assertTrue(list.getData().isEmpty());
  }

  @Test
  void constructor_withDocIDList_thenWrapsCorrectly() {
    DocIDJSON doc1 = new DocIDJSON("src1", 1);
    DocIDJSON doc2 = new DocIDJSON("src2", 2);
    DataListJSON<DocIDJSON> list = new DataListJSON<>(List.of(doc1, doc2));
    assertEquals(2, list.getData().size());
    assertEquals("src1", list.getData().get(0).getSource());
  }
}
