package iped.runner.execution;

import java.util.List;

public record RunRequest(List<RunToken> tokens) {

    /** A single CLI token: a flag and an optional raw (un-shell-quoted) value. */
    public record RunToken(String flag, String val) {}
}
