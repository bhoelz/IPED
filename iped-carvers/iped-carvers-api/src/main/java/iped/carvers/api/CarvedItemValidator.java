package iped.carvers.api;

import iped.data.IItem;

/**
 * Pluggable validator for carved items. Implementations are registered on a {@link CarverType} via
 * {@link CarverType#addValidator(CarvedItemValidator)}. All registered validators run inside {@link
 * iped.carvers.standard.AbstractCarver#isValid} before the carved item is accepted; if any returns
 * {@code false} the item is discarded.
 *
 * <p>Validators receive the raw bytes from the parent item at the carved range so they can perform
 * cheap header sanity checks without materialising the item. Implementations must be thread-safe
 * (one carver instance per worker thread).
 */
public interface CarvedItemValidator {

  /**
   * Returns {@code true} if the carved region starting at {@code offset} with length {@code length}
   * inside {@code parentEvidence} looks valid.
   *
   * @param parentEvidence the evidence item being carved
   * @param hit the header hit that triggered carving
   * @param length the computed length of the candidate carved item
   * @return {@code true} to accept, {@code false} to discard
   */
  boolean isValid(IItem parentEvidence, Hit hit, long length);
}
