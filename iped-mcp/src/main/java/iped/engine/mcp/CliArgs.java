package iped.engine.mcp;

public record CliArgs(String webapiUrl, String transport, int port) {

    public static CliArgs parse(String[] args) {
        String webapiUrl = null;
        String transport = "stdio";
        int port = 3000;

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
            } else {
                System.err.println("Unknown argument: " + arg);
                return null;
            }
        }

        if (webapiUrl == null) {
            System.err.println("Missing required argument: --webapi-url");
            return null;
        }

        return new CliArgs(webapiUrl, transport, port);
    }
}
