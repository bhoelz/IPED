package iped.engine.mcp;

import java.util.Arrays;
import java.util.Collections;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Parsed command-line arguments for the MCP server process.
 *
 * @param webapiUrl     base URL of iped-webapi (required)
 * @param transport     transport type: "stdio" (default)
 * @param port          HTTP port when transport=http (default 3000)
 * @param apiKey        optional Bearer token forwarded to iped-webapi
 * @param allowedCases  case-ID allowlist; empty = all open cases accessible
 * @param rateLimit     max tool invocations per minute per session (default 120)
 */
public record CliArgs(
        String      webapiUrl,
        String      transport,
        int         port,
        String      apiKey,
        Set<String> allowedCases,
        int         rateLimit
) {

    public static CliArgs parse(String[] args) {
        String      webapiUrl    = null;
        String      transport    = "stdio";
        int         port         = 3000;
        String      apiKey       = null;
        Set<String> allowedCases = Collections.emptySet();
        int         rateLimit    = 120;

        for (String arg : args) {
            if (arg.startsWith("--webapi-url=")) {
                webapiUrl = arg.substring("--webapi-url=".length());
            } else if (arg.startsWith("--transport=")) {
                transport = arg.substring("--transport=".length());
            } else if (arg.startsWith("--port=")) {
                try {
                    port = Integer.parseInt(arg.substring("--port=".length()));
                } catch (NumberFormatException e) {
                    System.err.println("Invalid port number: " + e.getMessage());
                    return null;
                }
            } else if (arg.startsWith("--api-key=")) {
                apiKey = arg.substring("--api-key=".length());
            } else if (arg.startsWith("--allowed-cases=")) {
                String csv = arg.substring("--allowed-cases=".length()).trim();
                if (!csv.isEmpty()) {
                    allowedCases = Arrays.stream(csv.split(","))
                            .map(String::trim)
                            .filter(s -> !s.isEmpty())
                            .collect(Collectors.toUnmodifiableSet());
                }
            } else if (arg.startsWith("--rate-limit=")) {
                try {
                    rateLimit = Integer.parseInt(arg.substring("--rate-limit=".length()));
                    if (rateLimit <= 0) throw new NumberFormatException("must be > 0");
                } catch (NumberFormatException e) {
                    System.err.println("Invalid rate-limit: " + e.getMessage());
                    return null;
                }
            } else {
                System.err.println("Unknown argument: " + arg);
                return null;
            }
        }

        if (webapiUrl == null) {
            System.err.println("Missing required argument: --webapi-url");
            return null;
        }

        return new CliArgs(webapiUrl, transport, port, apiKey, allowedCases, rateLimit);
    }
}
