package iped.search;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import org.junit.jupiter.api.Test;

class SearchQueryDefinitionTest {

    @Test
    void of_whenExpressionProvided_thenKeepsExpressionInAllAccessors() {
        SearchQueryDefinition definition = SearchQueryDefinition.of("name:test");

        assertEquals("name:test", definition.expression());
        assertEquals("name:test", definition.toString());
    }

    @Test
    void equalsAndHashCode_whenSameExpression_thenMatch() {
        SearchQueryDefinition a = SearchQueryDefinition.of("x");
        SearchQueryDefinition b = SearchQueryDefinition.of("x");
        SearchQueryDefinition c = SearchQueryDefinition.of("y");

        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());
        assertNotEquals(a, c);
    }

    @Test
    void equals_whenComparedWithDifferentType_thenReturnsFalse() {
        SearchQueryDefinition definition = SearchQueryDefinition.of("x");

        assertNotEquals(definition, "x");
    }
}

