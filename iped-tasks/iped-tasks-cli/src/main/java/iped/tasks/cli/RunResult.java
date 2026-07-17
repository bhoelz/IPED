package iped.tasks.cli;

import java.util.List;

public record RunResult(String taskClassName, String input, List<ItemResult> items) {

  public boolean hasAnyUnsupported() {
    return items.stream().anyMatch(i -> ItemResult.UNSUPPORTED.equals(i.status()));
  }

  public boolean hasAnyError() {
    return items.stream().anyMatch(i -> ItemResult.ERROR.equals(i.status()));
  }
}
