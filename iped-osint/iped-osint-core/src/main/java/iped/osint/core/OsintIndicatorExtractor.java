package iped.osint.core;

import iped.data.IItemReader;

import java.util.List;

public interface OsintIndicatorExtractor {

    List<OsintIndicator> extract(IItemReader item);
}
