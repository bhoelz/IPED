package iped.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.Date;
import org.junit.jupiter.api.Test;

class TimeConverterTest {

  // Windows FILETIME for 1970-01-01 00:00:00 UTC = 116444736000000000L
  private static final long EPOCH_FILETIME = 116444736000000000L;

  @Test
  void filetimeToMillis_whenEpochFiletime_thenZero() {
    assertEquals(0L, TimeConverter.filetimeToMillis(EPOCH_FILETIME));
  }

  @Test
  void filetimeToMillis_whenOneSecondAfterEpoch_thenThousandMillis() {
    long oneSecond = EPOCH_FILETIME + 10_000_000L; // 10M * 100ns = 1s
    assertEquals(1000L, TimeConverter.filetimeToMillis(oneSecond));
  }

  @Test
  void filetimeToMillis_whenBeforeEpoch_thenZero() {
    // Negative filetime (before 1970) clamps to 0
    assertEquals(0L, TimeConverter.filetimeToMillis(0L));
  }

  @Test
  void fileTimeToDate_whenEpochFiletime_thenJavaEpoch() {
    Date d = TimeConverter.fileTimeToDate(EPOCH_FILETIME);
    assertEquals(0L, d.getTime());
  }

  @Test
  void fileTimeToDate_longArray_whenEpochFiletime_thenJavaEpoch() {
    // EPOCH_FILETIME as low/high 32-bit words
    long low = EPOCH_FILETIME & 0xFFFFFFFFL;
    long high = (EPOCH_FILETIME >>> 32) & 0xFFFFFFFFL;
    Date d = TimeConverter.fileTimeToDate(new long[] {low, high});
    assertEquals(0L, d.getTime());
  }

  @Test
  void unixTimeToDate_whenZero_thenJavaEpoch() {
    Date d = TimeConverter.unixTimeToDate(0L);
    assertEquals(0L, d.getTime());
  }

  @Test
  void unixTimeToDate_whenOneSecond_thenThousandMillis() {
    Date d = TimeConverter.unixTimeToDate(1L);
    assertEquals(1000L, d.getTime());
  }

  @Test
  void PRTimeToDate_whenOneMicrosecond_thenOneMillisecond() {
    // PRTime is microseconds; 1000 µs = 1 ms past epoch
    Date d = TimeConverter.PRTimeToDate(1000L);
    assertEquals(1L, d.getTime());
  }

  @Test
  void systemTimeToDate_returnsNonNull() {
    // systemTimeToDate is used for Windows SYSTEMTIME; just assert non-null
    Date d = TimeConverter.systemTimeToDate(11644473600000L * 1000L);
    assertNotNull(d);
  }
}
