package iped.app.timelinegraph.cache;

import org.jfree.data.time.TimePeriod;

import java.util.*;

public interface TimeStampCache extends Runnable {
    public void addTimePeriodClassToCache(Class<? extends TimePeriod> timePeriodClass);

    public boolean hasTimePeriodClassToCache(Class<? extends TimePeriod> timePeriodClass);

    public ArrayList<Class<? extends TimePeriod>> getPeriodClassesToCache();

    public Map<String, List<CacheTimePeriodEntry>> getCachedList();

    public Map<String, Set<CacheTimePeriodEntry>> getNewCache();

    public TimeZone getCacheTimeZone();
}
