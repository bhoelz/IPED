package iped.datasource;

/** Strategy used when distributed workers emit one additional-store segment. */
public enum SegmentOutputStrategy { MERGE_BY_ITEM, LATE_INDEX }
