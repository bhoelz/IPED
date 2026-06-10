package iped.search;

import java.util.Objects;

/**
 * Neutral query representation for API contracts, wrapping a query expression
 * string without depending on a specific query parser.
 */
public final class SearchQueryDefinition {

    private final String expression;

    private SearchQueryDefinition(String expression) {
        this.expression = expression;
    }

    /**
     * Creates a query definition from a query expression string.
     *
     * @param expression the query expression
     * @return a new instance wrapping the given expression
     */
    public static SearchQueryDefinition of(String expression) {
        return new SearchQueryDefinition(expression);
    }

    /**
     * @return the wrapped query expression
     */
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
        if (!(obj instanceof SearchQueryDefinition other)) {
            return false;
        }
        return Objects.equals(expression, other.expression);
    }
}
