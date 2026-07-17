/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package iped.exception;

import java.io.Serial;

/**
 * Thrown when a search query string cannot be parsed into a valid query. Decouples API clients from
 * the underlying query parser implementation.
 *
 * @author WERNECK
 */
public class QueryNodeException extends Exception {

  @Serial private static final long serialVersionUID = 1L;

  /**
   * Creates an exception wrapping the parser-specific cause.
   *
   * @param cause the underlying query parsing failure
   */
  public QueryNodeException(Exception cause) {
    super(cause);
  }
}
