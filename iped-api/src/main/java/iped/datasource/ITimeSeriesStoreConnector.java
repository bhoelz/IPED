package iped.datasource;

import java.io.IOException;
import java.time.Instant;
import java.util.List;

/** Connector contract for timeline events associated with evidence items. */
public interface ITimeSeriesStoreConnector extends IAdditionalStoreConnector {
  void appendEvent(TimelineEvent event) throws IOException;

  List<TimelineEvent> query(Instant from, Instant to, Integer itemId, int limit) throws IOException;
}
