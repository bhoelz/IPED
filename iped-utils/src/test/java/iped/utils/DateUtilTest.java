package iped.utils;

import static org.junit.jupiter.api.Assertions.*;

import java.text.ParseException;
import java.util.Date;
import org.junit.jupiter.api.Test;

class DateUtilTest {

  @Test
  void tryToParseDate_whenValidIsoUtcZ_thenReturnsDate() {
    Date d = DateUtil.tryToParseDate("2023-06-15T10:30:00Z");
    assertNotNull(d);
  }

  @Test
  void tryToParseDate_whenValidIsoWithTimezone_thenReturnsDate() {
    Date d = DateUtil.tryToParseDate("2023-06-15T10:30:00-0300");
    assertNotNull(d);
  }

  @Test
  void tryToParseDate_whenValidIsoWithColonTimezone_thenReturnsDate() {
    Date d = DateUtil.tryToParseDate("2023-06-15T10:30:00-03:00");
    assertNotNull(d);
  }

  @Test
  void tryToParseDate_whenIsoWithSpaceSeparator_thenReturnsDate() {
    Date d = DateUtil.tryToParseDate("2023-06-15 10:30:00Z");
    assertNotNull(d);
  }

  @Test
  void tryToParseDate_whenColonDateFormat_thenReturnsDate() {
    Date d = DateUtil.tryToParseDate("2023:06:15 10:30:00");
    assertNotNull(d);
  }

  @Test
  void tryToParseDate_whenInvalidString_thenNull() {
    assertNull(DateUtil.tryToParseDate("not-a-date"));
  }

  @Test
  void tryToParseDate_whenTooShort_thenNull() {
    assertNull(DateUtil.tryToParseDate("2023-06"));
  }

  @Test
  void dateToString_andStringToDate_roundTrip() throws ParseException {
    String original = "2023-06-15T10:30:00Z";
    Date date = DateUtil.stringToDate(original);
    String result = DateUtil.dateToString(date);
    assertEquals(original, result);
  }

  @Test
  void tryToParseDate_whenThreadSafe_thenReturnsCorrectDate() throws InterruptedException {
    // Minimal thread-safety smoke test: two threads parse the same date string
    Date[] results = new Date[2];
    Thread t1 = new Thread(() -> results[0] = DateUtil.tryToParseDate("2020-01-01T00:00:00Z"));
    Thread t2 = new Thread(() -> results[1] = DateUtil.tryToParseDate("2021-12-31T23:59:59Z"));
    t1.start();
    t2.start();
    t1.join();
    t2.join();
    assertNotNull(results[0]);
    assertNotNull(results[1]);
    assertNotEquals(results[0], results[1]);
  }
}
