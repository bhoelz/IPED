package iped.search;

import java.util.Objects;

/**
 * Neutral query representation for API contracts.
 */
public final class SearchQueryDefinition {

    private final String expression;

    private SearchQueryDefinition(String expression) {
        this.expression = expression;
    }

    public static SearchQueryDefinition of(String expression) {
        return new SearchQueryDefinition(expression);
    }

    public String expression() {
        return expression;
    }

    @Override
    public String toString() {
        return expression;
    }

    @Override
    public int hashCode() {
        return Objects.hash(expression);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof SearchQueryDefinition)) {
            return false;
        }
        SearchQueryDefinition other = (SearchQueryDefinition) obj;
        return Objects.equals(expression, other.expression);
    }
}
