package iped.exception;

import java.io.Serial;

public class IPEDException extends RuntimeException {

    @Serial
    private static final long serialVersionUID = 1L;

    public IPEDException(String msg) {
        super(msg);
    }

    public IPEDException(String msg, Throwable e) {
        super(msg, e);
    }

    public IPEDException(Throwable e) {
        super(e);
    }

}
