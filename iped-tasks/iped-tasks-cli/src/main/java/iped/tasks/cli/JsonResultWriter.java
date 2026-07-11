package iped.tasks.cli;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import java.io.IOException;
import java.io.PrintStream;

public class JsonResultWriter implements ResultWriter {

    private final ObjectMapper mapper = new ObjectMapper()
            .enable(SerializationFeature.INDENT_OUTPUT);

    @Override
    public void write(RunResult result, PrintStream out) throws IOException {
        out.println(mapper.writeValueAsString(result));
    }
}
