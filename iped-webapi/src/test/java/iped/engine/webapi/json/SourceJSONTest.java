package iped.engine.webapi.json;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class SourceJSONTest {

    // --- SourceJSON ---

    @Test
    void sourceJSON_settersAndGetters_roundTrip() {
        SourceJSON src = new SourceJSON();
        src.setId("source-01");
        src.setPath("/evidence/disk.dd");

        assertEquals("source-01", src.getId());
        assertEquals("/evidence/disk.dd", src.getPath());
    }

    @Test
    void sourceJSON_defaults_whenNew_thenNull() {
        SourceJSON src = new SourceJSON();
        assertNull(src.getId());
        assertNull(src.getPath());
    }

    // --- SourceToIDsJSON ---

    @Test
    void sourceToIDsJSON_defaultConstructor_thenEmptyData() {
        SourceToIDsJSON stoi = new SourceToIDsJSON();
        assertNotNull(stoi.getData());
        assertTrue(stoi.getData().isEmpty());
    }

    @Test
    void sourceToIDsJSON_fromDocList_groupsBySource() {
        List<DocIDJSON> docs = List.of(
                new DocIDJSON("srcA", 1),
                new DocIDJSON("srcA", 2),
                new DocIDJSON("srcB", 3)
        );
        SourceToIDsJSON stoi = new SourceToIDsJSON(docs);
        List<DocIDGroupJSON> groups = stoi.getData();

        // Two distinct sources
        assertEquals(2, groups.size());

        // srcA has 2 IDs, srcB has 1
        DocIDGroupJSON srcAGroup = groups.stream()
                .filter(g -> "srcA".equals(g.getSource()))
                .findFirst()
                .orElse(null);
        assertNotNull(srcAGroup);
        assertEquals(2, srcAGroup.getIds().size());
        assertTrue(srcAGroup.getIds().containsAll(List.of(1, 2)));
    }

    @Test
    void sourceToIDsJSON_setData_thenGetDataReturnsIt() {
        SourceToIDsJSON stoi = new SourceToIDsJSON();
        DocIDGroupJSON grp = new DocIDGroupJSON("srcC", List.of(10, 20, 30));
        stoi.setData(List.of(grp));

        List<DocIDGroupJSON> result = stoi.getData();
        assertEquals(1, result.size());
        assertEquals("srcC", result.get(0).getSource());
    }
}
