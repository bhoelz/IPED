package iped.carvers.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import iped.carvers.api.Signature.SignatureType;
import org.junit.jupiter.api.Test;

public class HitTest {

  private static Signature signature(String carverName) {
    CarverType type = new CarverType();
    type.setName(carverName);
    return new Signature(type, "sig", SignatureType.HEADER);
  }

  @Test
  public void exposesOffsetAndSignature() {
    Signature sig = signature("jpeg");
    Hit hit = new Hit(sig, 42L);

    assertEquals(42L, hit.getOffset());
    assertSame(sig, hit.getSignature());
  }

  @Test
  public void hitsAreEqualWhenOffsetAndCarverNameMatch() {
    Hit a = new Hit(signature("jpeg"), 10L);
    Hit b = new Hit(signature("jpeg"), 10L);
    Hit differentOffset = new Hit(signature("jpeg"), 11L);
    Hit differentCarver = new Hit(signature("png"), 10L);

    assertEquals(a, b);
    assertNotEquals(a, differentOffset);
    assertNotEquals(a, differentCarver);
    assertNotEquals(a, "not a hit");
  }

  @Test
  public void toStringIncludesOffset() {
    Hit hit = new Hit(signature("jpeg"), 99L);
    assertTrue(hit.toString().contains("Offset:99"));
  }
}
