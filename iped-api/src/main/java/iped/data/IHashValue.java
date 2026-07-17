/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package iped.data;

import java.io.Serial;
import java.io.Serializable;

/**
 * A hash value (MD5, SHA-1, etc.) backed by its raw bytes, comparable and usable as a map key. Two
 * hash values are equal when their bytes are equal.
 *
 * @author Nassif
 */
public abstract class IHashValue implements Comparable<IHashValue>, Serializable {

  @Serial private static final long serialVersionUID = 1L;

  /**
   * @return the raw bytes of this hash value
   */
  public abstract byte[] getBytes();

  /**
   * @return this hash encoded as an upper-case hexadecimal string
   */
  public String toString() {
    StringBuilder result = new StringBuilder();
    for (byte b : getBytes()) {
      result.append(String.format("%1$02X", b)); // $NON-NLS-1$
    }
    return result.toString();
  }

  /**
   * Compares hashes byte by byte, treating each byte as unsigned.
   *
   * @param hash the hash to compare against; must have the same length
   * @return a negative integer, zero, or a positive integer as this hash is less than, equal to, or
   *     greater than the given one
   */
  @Override
  public int compareTo(IHashValue hash) {
    byte[] compBytes = hash.getBytes();
    byte[] bytes = getBytes();
    for (int i = 0; i < bytes.length; i++) {
      int cmp = Integer.compare(bytes[i] & 0xFF, compBytes[i] & 0xFF);
      if (cmp != 0) return cmp;
    }
    return 0;
  }

  @Override
  public boolean equals(Object obj) {
    if (obj == this) return true;
    if (obj == null) return false;
    if (getClass() != obj.getClass()) return false;
    IHashValue other = (IHashValue) obj;
    return compareTo(other) == 0;
  }

  @Override
  public int hashCode() {
    byte[] bytes = getBytes();
    return bytes[3] & 0xFF
        | (bytes[2] & 0xFF) << 8
        | (bytes[1] & 0xFF) << 16
        | (bytes[0] & 0xFF) << 24;
  }
}
