package iped.tasks.cli;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class ResultWriterTest {

  private static RunResult sampleResult() {
    ItemResult ok =
        ItemResult.ok(
            "/tmp/a.txt",
            12,
            Map.of("Content-Type", new String[] {"text/plain"}),
            Map.of("sha-256", "abc123"));
    ItemResult unsupported =
        ItemResult.unsupported("/tmp/b.txt", "depends on case/index (test reason)");
    return new RunResult("iped.engine.task.HashTask", "2 item(s)", List.of(ok, unsupported));
  }

  @Test
  void stdoutWriterIncludesPathsStatusesAndAttributes() {
    StdoutResultWriter writer = new StdoutResultWriter();
    ByteArrayOutputStream buffer = new ByteArrayOutputStream();

    writer.write(sampleResult(), new PrintStream(buffer, true, StandardCharsets.UTF_8));

    String output = buffer.toString(StandardCharsets.UTF_8);
    assertTrue(output.contains("/tmp/a.txt | OK"));
    assertTrue(output.contains("/tmp/b.txt | UNSUPPORTED | depends on case/index (test reason)"));
    assertTrue(output.contains("sha-256 = abc123"));
    assertTrue(output.contains("1 ok, 1 unsupported, 0 errors (of 2)"));
  }

  @Test
  void jsonWriterProducesParseableJsonWithExpectedFields() throws Exception {
    JsonResultWriter writer = new JsonResultWriter();
    ByteArrayOutputStream buffer = new ByteArrayOutputStream();

    writer.write(sampleResult(), new PrintStream(buffer, true, StandardCharsets.UTF_8));

    JsonNode root = new ObjectMapper().readTree(buffer.toString(StandardCharsets.UTF_8));
    assertEquals("iped.engine.task.HashTask", root.get("taskClassName").asText());
    assertEquals(2, root.get("items").size());
    assertEquals("OK", root.get("items").get(0).get("status").asText());
    assertEquals("abc123", root.get("items").get(0).get("extraAttributes").get("sha-256").asText());
    assertEquals("UNSUPPORTED", root.get("items").get(1).get("status").asText());
  }
}
