package iped.app.timelinegraph.datasets;

import iped.app.timelinegraph.IpedChartsPanel;
import iped.app.timelinegraph.cache.IndexTimeStampCache;
import iped.app.timelinegraph.cache.TimeStampCache;
import iped.jfextensions.model.Minute;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.*;
import lombok.extern.slf4j.Slf4j;
import org.jfree.data.time.*;
import org.jfree.data.xy.AbstractIntervalXYDataset;

/*
 * Implements the method to choose timeline dataset object that represents.
 *
 * Obs.: Currently it checks if there is an available cache. If not use a dataset with direct access to lucene resultset.
 */
@Slf4j
public class IpedTimelineDatasetManager {
  IpedChartsPanel ipedChartsPanel;

  List<TimeStampCache> timeStampCaches = new ArrayList<>();
  volatile boolean isCacheLoaded = false;
  TimeStampCache selectedTimeStampCache;

  public IpedTimelineDatasetManager(IpedChartsPanel ipedChartsPanel) {
    this.ipedChartsPanel = ipedChartsPanel;

    List<Class<? extends TimePeriod>> periods =
        Arrays.asList(
            Day.class,
            Hour.class,
            Year.class,
            Month.class,
            Quarter.class,
            Week.class,
            Minute.class,
            Second.class);

    for (Class<? extends TimePeriod> period : periods) {
      TimeStampCache timeStampCache =
          new IndexTimeStampCache(ipedChartsPanel, ipedChartsPanel.getResultsProvider());
      timeStampCache.addTimePeriodClassToCache(period);
      timeStampCaches.add(timeStampCache);
    }
  }

  public AbstractIntervalXYDataset getBestDataset(
      Class<? extends TimePeriod> timePeriodClass, String splitValue) {
    try {
      for (TimeStampCache timeStampCache : timeStampCaches) {
        if (timeStampCache.hasTimePeriodClassToCache(timePeriodClass)) {
          selectedTimeStampCache = timeStampCache;
          return new IpedTimelineDataset(this, ipedChartsPanel.getResultsProvider(), splitValue);
        }
      }
      return null;
    } catch (Exception e) {
      e.printStackTrace();
    }
    return null;
  }

  /*
   * Start the creation of cache for timeline chart
   */
  public void startCacheCreation() {
    try {
      Thread.sleep(1000);
    } catch (InterruptedException e) {
      e.printStackTrace();
    }

    int poolSize = 1;
    int totalItems = ipedChartsPanel.getResultsProvider().getIPEDSource().getTotalItems();
    if (getAvailableMemory() > totalItems * 100) {
      poolSize = (int) Math.ceil((float) Runtime.getRuntime().availableProcessors() / 2f);
    } else {
      log.info(
          "Only {}MB of free memory for {} total items. Timeline index creation will occur sequentially. ",
          Runtime.getRuntime().freeMemory(),
          totalItems);
    }
    ExecutorService threadPool = Executors.newFixedThreadPool(poolSize);
    boolean first = true;
    for (TimeStampCache timeStampCache : timeStampCaches) {
      Future<?> future = threadPool.submit(timeStampCache);
      // first loads the Day cache alone to speed up it, then run others in parallel
      if (first) {
        first = false;
        try {
          future.get();
        } catch (InterruptedException | ExecutionException e) {
          e.printStackTrace();
        }
      }
    }
    threadPool.shutdown();
    try {
      threadPool.awaitTermination(12, TimeUnit.HOURS);
    } catch (InterruptedException e) {
      e.printStackTrace();
    }
  }

  public TimeStampCache getCache() {
    return selectedTimeStampCache;
  }

  public IpedChartsPanel getIpedChartsPanel() {
    return ipedChartsPanel;
  }

  public void waitMemory() throws InterruptedException {
    if (getAvailableMemory() < Runtime.getRuntime().maxMemory() / 2) {
      while (!isCacheLoaded) {
        Thread.sleep(100);
      }
      int tries = 0;
      while (getAvailableMemory() < 40000000) {
        Thread.sleep(1000);
        if (++tries > 30) {
          throw new OutOfMemoryError();
        }
        System.gc();
      }
    }
  }

  public boolean isCacheLoaded() {
    return isCacheLoaded;
  }

  public void setCacheLoaded(boolean isCacheLoaded) {
    this.isCacheLoaded = isCacheLoaded;
  }

  public static long getAvailableMemory() {
    return Runtime.getRuntime().freeMemory()
        + Runtime.getRuntime().maxMemory()
        - Runtime.getRuntime().totalMemory();
  }
}
