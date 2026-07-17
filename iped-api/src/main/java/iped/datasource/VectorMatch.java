package iped.datasource;

/** One similarity result mapped back to an original evidence item. */
public record VectorMatch(int itemId, float score) { }
