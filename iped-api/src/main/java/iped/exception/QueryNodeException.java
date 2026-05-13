/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package iped.exception;

import java.io.Serial;

/**
 *
 * @author WERNECK
 */
public class QueryNodeException extends Exception {

    @Serial
    private static final long serialVersionUID = 1L;

    public QueryNodeException(Exception cause) {
        super(cause);
    }

}
