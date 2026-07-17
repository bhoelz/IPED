package iped.tasks.cli;

import java.io.PrintStream;
import java.util.Map;

public class StdoutResultWriter implements ResultWriter {

  @Override
  public void write(RunResult result, PrintStream out) {
    out.println("task: " + result.taskClassName());
    out.println("input: " + result.input());
    out.println();

    for (ItemResult item : result.items()) {
      out.printf(
          "%s | %s%s%n",
          item.path(), item.status(), item.reason() != null ? " | " + item.reason() : "");
      if (ItemResult.OK.equals(item.status())) {
        if (item.metadata() != null) {
          for (Map.Entry<String, String[]> e : item.metadata().entrySet()) {
            out.println("    metadata: " + e.getKey() + " = " + String.join(", ", e.getValue()));
          }
        }
        if (item.extraAttributes() != null) {
          for (Map.Entry<String, Object> e : item.extraAttributes().entrySet()) {
            out.println("    attribute: " + e.getKey() + " = " + e.getValue());
          }
        }
      }
    }

    long ok = result.items().stream().filter(i -> ItemResult.OK.equals(i.status())).count();
    long unsupported =
        result.items().stream().filter(i -> ItemResult.UNSUPPORTED.equals(i.status())).count();
    long errors = result.items().stream().filter(i -> ItemResult.ERROR.equals(i.status())).count();
    out.println();
    out.printf(
        "summary: %d ok, %d unsupported, %d errors (of %d)%n",
        ok, unsupported, errors, result.items().size());
  }
}
