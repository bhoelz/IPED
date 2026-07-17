package iped.runner.execution;

import java.util.List;

public record RunRequest(List<RunToken> tokens, RunPriority priority, String profile) {
  /** Convenience constructor for callers that don't need priority/profile. */
  public RunRequest(List<RunToken> tokens) {
    this(tokens, RunPriority.NORMAL, null);
  }

  /** A single CLI token: a flag and an optional raw (un-shell-quoted) value. */
  public record RunToken(String flag, String val) {}
}
