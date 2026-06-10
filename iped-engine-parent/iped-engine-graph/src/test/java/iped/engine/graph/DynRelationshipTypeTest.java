package iped.engine.graph;

import org.junit.jupiter.api.Test;
import org.neo4j.graphdb.RelationshipType;

import static org.junit.jupiter.api.Assertions.*;

class DynRelationshipTypeTest {

    @Test
    void constructor_storesName() {
        DynRelationshipType rel = new DynRelationshipType("KNOWS");
        assertEquals("KNOWS", rel.name());
    }

    @Test
    void name_returnsExactString() {
        DynRelationshipType rel = new DynRelationshipType("CALLED");
        assertEquals("CALLED", rel.name());
    }

    @Test
    void factory_withName_createsInstance() {
        RelationshipType rel = DynRelationshipType.withName("SENT_EMAIL");
        assertNotNull(rel);
        assertEquals("SENT_EMAIL", rel.name());
    }

    @Test
    void factory_withName_returnsRelationshipType() {
        RelationshipType rel = DynRelationshipType.withName("PROXIMITY");
        assertInstanceOf(DynRelationshipType.class, rel);
    }

    @Test
    void toString_containsName() {
        DynRelationshipType rel = new DynRelationshipType("OWNS");
        String str = rel.toString();
        assertNotNull(str);
        assertTrue(str.contains("OWNS"), "toString should contain the relationship type name");
    }

    @Test
    void twoTypes_withDifferentNames_areDistinct() {
        RelationshipType r1 = DynRelationshipType.withName("KNOWS");
        RelationshipType r2 = DynRelationshipType.withName("HATES");
        assertNotEquals(r1.name(), r2.name());
    }
}
