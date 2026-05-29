package iped.engine.config.api;

/**
 * Minimal launcher for ConfigurationServer that avoids full application initialization.
 */
public class ConfigurationServerLauncher {
    public static void main(String[] args) {
        int port = 8080;
        if (args.length > 0) {
            try {
                port = Integer.parseInt(args[0]);
            } catch (NumberFormatException e) {
                System.err.println("Invalid port: " + args[0]);
                System.exit(1);
            }
        }

        try {
            ConfigurationServer server = new ConfigurationServer(port);
            server.start();
            System.out.println("Configuration API running on http://localhost:" + port + "/api/v1");
            System.out.println("Press Ctrl+C to stop");
            server.join();
        } catch (Exception e) {
            System.err.println("Failed to start server: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }
}
