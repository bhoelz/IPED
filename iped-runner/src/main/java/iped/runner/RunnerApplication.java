package iped.runner;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * IPED Runner — CLI parameter configuration UI.
 *
 * <p>Spring Boot serves the SSR shell and static assets. The interactive
 * config form is a React island loaded by the Rocker page template; it
 * assembles and validates the IPED command line entirely in the browser.
 */
@SpringBootApplication
public class RunnerApplication {

    public static void main(String[] args) {
        SpringApplication.run(RunnerApplication.class, args);
    }
}
