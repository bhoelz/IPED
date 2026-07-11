package iped.tasks.cli;

import java.io.IOException;
import java.io.PrintStream;

public interface ResultWriter {

    void write(RunResult result, PrintStream out) throws IOException;
}
